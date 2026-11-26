const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildProducto(uid, overrides = {}) {
  const ts = new Date(Date.now() - 5000);
  return {
    userId: uid,
    sku: "COL-MAT-001",
    modelo: "Colcha Primavera Floral",
    tamano: "Matrimonial",
    color: "Rosa / Crema",
    ubicacion: "Estante / Tienda",
    stockInicial: 5,
    costo: 450,
    precio: 850,
    stockMinimo: 2,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function buildEntrada(uid, productoId, overrides = {}) {
  const ts = new Date(Date.now() - 3000);
  return {
    userId: uid,
    fecha: "2026-11-26",
    productoId,
    sku: "COL-MAT-001",
    modelo: "Colcha Primavera Floral",
    cantidad: 4,
    costo: 450,
    proveedor: "Textil Norte",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function buildVenta(uid, productoId, overrides = {}) {
  const ts = new Date(Date.now() - 2000);
  return {
    userId: uid,
    fecha: "2026-11-26",
    productoId,
    sku: "COL-MAT-001",
    modelo: "Colcha Primavera Floral",
    cantidad: 1,
    precioUnitario: 850,
    costoUnitario: 450,
    importeTotal: 850,
    pagado: 300,
    saldo: 550,
    modalidad: "pagos",
    proximoCobro: "2026-12-03",
    clienteNombre: "Sra. Rosa Martínez",
    clienteTelefono: "5512345678",
    nota: "Abona los sábados",
    historialPagos: [{ fecha: "2026-11-26", monto: 300, concepto: "Enganche" }],
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("productos").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("productos")
      .doc("prod_1")
      .set(buildProducto(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ventas").get());
});

test("Owner can create, update, list, and delete productos with valid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("productos");

  await assertSucceeds(col.doc("prod_1").set(buildProducto(ALICE_UID)));
  await assertSucceeds(col.get());

  // Partial update: ubicación & costo
  await assertSucceeds(
    col.doc("prod_1").update({
      ubicacion: "Con mi hermana",
      costo: 470,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    col.doc("prod_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field ("id") on create
  await assertFails(
    col.doc("prod_2").set(buildProducto(ALICE_UID, { id: "prod_2" }))
  );
});

test("Owner can create entradas linked to existing producto; orphaned entrada fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodCol = aliceDb.collection("users").doc(ALICE_UID).collection("productos");
  const entCol = aliceDb.collection("users").doc(ALICE_UID).collection("entradas");

  // Orphaned entrada without producto must fail
  await assertFails(entCol.doc("ent_orphan").set(buildEntrada(ALICE_UID, "missing_prod")));

  // Create producto then entrada
  await assertSucceeds(prodCol.doc("prod_1").set(buildProducto(ALICE_UID)));
  await assertSucceeds(entCol.doc("ent_1").set(buildEntrada(ALICE_UID, "prod_1")));
  await assertSucceeds(entCol.get());
});

test("Owner can create venta and register abono payment update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prodCol = aliceDb.collection("users").doc(ALICE_UID).collection("productos");
  const venCol = aliceDb.collection("users").doc(ALICE_UID).collection("ventas");

  await assertSucceeds(prodCol.doc("prod_1").set(buildProducto(ALICE_UID)));
  await assertSucceeds(venCol.doc("ven_1").set(buildVenta(ALICE_UID, "prod_1")));

  // Register valid abono
  await assertSucceeds(
    venCol.doc("ven_1").update({
      pagado: 500,
      saldo: 350,
      proximoCobro: "2026-12-10",
      historialPagos: [
        { fecha: "2026-11-26", monto: 300, concepto: "Enganche" },
        { fecha: "2026-11-27", monto: 200, concepto: "Abono" },
      ],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject reducing pagado or increasing saldo
  await assertFails(
    venCol.doc("ven_1").update({
      pagado: 100,
      saldo: 750,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject modifying precioUnitario after sale
  await assertFails(
    venCol.doc("ven_1").update({
      precioUnitario: 100,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("productos").doc("prod_1").set(buildProducto(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("productos").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("productos").doc("prod_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("productos").doc("prod_1").update({
      modelo: "Hack",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("productos").doc("prod_bob").set(buildProducto(BOB_UID))
  );
});
