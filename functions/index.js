const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyMatchingAlerts = onDocumentWritten("services/{serviceId}", async (event) => {
  const afterData = event.data?.after?.data();
  if (!afterData) return;

  const depotName = afterData.current_depot;
  const platformNumber = afterData.current_platform;

  if (!depotName || !platformNumber) return;

  const db = getFirestore();
  const alertsSnapshot = await db
    .collection("bus_alerts")
    .where("depot_name", "==", depotName)
    .where("platform_number", "==", platformNumber)
    .get();

  if (alertsSnapshot.empty) {
    console.log("No matching alerts found.");
    return;
  }

  const tokens = [];
  alertsSnapshot.forEach((doc) => {
    const data = doc.data();
    if (data.token) tokens.push(data.token);
  });

  if (tokens.length === 0) {
    console.log("No tokens found for matching alerts.");
    return;
  }

  const payload = {
    notification: {
      title: "Bus Alert",
      body: `A bus has arrived at Depot ${depotName}, Platform ${platformNumber}!`,
    },
  };

  try {
    const response = await getMessaging().sendToDevice(tokens, payload);
    console.log("Notification sent:", response);
  } catch (error) {
    console.error("Error sending notification:", error);
  }
});