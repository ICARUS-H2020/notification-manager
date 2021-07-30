package aero.icarus2020.consumers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.managers.*;
import aero.icarus2020.models.NotificationEvent;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationListener {

    @Autowired
    private DataCheckinJobCompletedManager dataCheckinJobCompletedManager;

    @Autowired
    private DataCheckinJobFailedManager dataCheckinJobFailedManager;

    @Autowired
    private DatasetUpdateCompletedManager datasetUpdateCompleted;

    @Autowired
    private DatasetUpdateFailedManager datasetUpdateFailed;

    @Autowired
    private InformDatasetUpdatedManager informDatasetUpdatedManager;

    @Autowired
    private DatasetDownloadCompletedManager datasetDownloadCompletedManager;

    @Autowired
    private DatasetDownloadFailedManager datasetDownloadFailedManager;

    @Autowired
    private JobCompletedManager jobCompletedManager;

    @Autowired
    private JobStartedManager jobStartedManager;

    @Autowired
    private JobFailedManager jobFailedManager;

    @Autowired
    private ContractAcceptedManager contractAcceptedManager;

    @Autowired
    private ContractOfferedManager contractOfferedManager;

    @Autowired
    private ContractPaidManager contractPaidManager;

    @Autowired
    private ContractRejectedManager contractRejectedManager;

    @Autowired
    private ContractNegotiatedManager contractNegotiatedManager;

    @Autowired
    private ContractCounteredManager contractCounteredManager;

    @Autowired
    private ContractOfferAcceptedManager contractOfferAcceptedManager;

    @Autowired
    private ContractOfferRejectedManager contractOfferRejectedManager;

    @Autowired
    private RequestManager requestManager;

    @Autowired
    private RequestRejectedManager requestRejectedManager;

    @Autowired
    private SuggestionAcceptedManager suggestionAcceptedManager;

    @Autowired
    private SuggestionRejectedManager suggestionRejectedManager;

    @Autowired
    private OtherManager otherManager;

    @KafkaListener(topics = "${kafka.topic.notifications}")
    public void receive(@Payload String data, @Headers MessageHeaders headers) throws ExecutionException, InterruptedException, MissingAttributeException {

        try {
            // Convert string to json
            data = data.startsWith("\"") ? data.substring(1) : data;
            data = data.endsWith("\"") ? data.substring(0, data.length() - 1) : data;
            data = data.replace("\\", "");
            JSONObject jsonData = new JSONObject(data);

            // build notification event
            NotificationEvent notifData = new NotificationEvent();

            if (!jsonData.has("eventType")) throw new MissingAttributeException("event type");
            notifData.setEventType(jsonData.getString("eventType"));
            if (!jsonData.has("properties")) throw new MissingAttributeException("properties");
            JSONObject properties = new JSONObject(jsonData.get("properties").toString());
            HashMap<String, String> propertiesMap = new HashMap<>();
            Iterator<String> keys = properties.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                propertiesMap.put(key, properties.get(key).toString());
            }

            notifData.setProperties(propertiesMap);

            switch (notifData.getEventType()) {
                case "DATA_CHECKIN_JOB_COMPLETED":
                    dataCheckinJobCompletedManager.setData(notifData);
                    dataCheckinJobCompletedManager.sendNotification();
                    break;
                case "DATA_CHECKIN_JOB_FAILED":
                    dataCheckinJobFailedManager.setData(notifData);
                    dataCheckinJobFailedManager.sendNotification();
                    break;
                case "DATA_ASSET_UPDATE_COMPLETED":
                    datasetUpdateCompleted.setData(notifData);
                    datasetUpdateCompleted.sendNotification();
                    break;
                case "DATA_ASSET_UPDATE_FAILED":
                    datasetUpdateFailed.setData(notifData);
                    datasetUpdateFailed.sendNotification();
                    break;
                case "INFORM_DATASET_UPDATED":
                    informDatasetUpdatedManager.setData(notifData);
                    informDatasetUpdatedManager.sendNotification();
                    break;
                case "DATASET_DOWNLOAD_COMPLETED":
                    datasetDownloadCompletedManager.setData(notifData);
                    datasetDownloadCompletedManager.sendNotification();
                    break;
                case "DATASET_DOWNLOAD_FAILED":
                    datasetDownloadFailedManager.setData(notifData);
                    datasetDownloadFailedManager.sendNotification();
                    break;
                case "JOB_STARTED":
                    jobStartedManager.setData(notifData);
                    jobStartedManager.sendNotification();
                    break;
                case "JOB_COMPLETED":
                    jobCompletedManager.setData(notifData);
                    jobCompletedManager.sendNotification();
                    break;
                case "JOB_FAILED":
                    jobFailedManager.setData(notifData);
                    jobFailedManager.sendNotification();
                    break;
                case "REQUEST":
                    requestManager.setData(notifData);
                    requestManager.sendNotification();
                    break;
                case "REQUEST_REJECTED":
                    requestRejectedManager.setData(notifData);
                    requestRejectedManager.sendNotification();
                    break;
                case "CONTRACT_OFFERED":
                    contractOfferedManager.setData(notifData);
                    contractOfferedManager.sendNotification();
                    break;
                case "CONTRACT_ACCEPTED":
                    contractAcceptedManager.setData(notifData);
                    contractAcceptedManager.sendNotification();
                    break;
                case "CONTRACT_REJECTED":
                    contractRejectedManager.setData(notifData);
                    contractRejectedManager.sendNotification();
                    break;
                case "CONTRACT_PAID":
                    contractPaidManager.setData(notifData);
                    contractPaidManager.sendNotification();
                    break;
                case "CONTRACT_NEGOTIATED":
                    contractNegotiatedManager.setData(notifData);
                    contractNegotiatedManager.sendNotification();
                    break;
                case "CONTRACT_COUNTERED":
                    contractCounteredManager.setData(notifData);
                    contractCounteredManager.sendNotification();
                    break;
                case "CONTRACT_OFFER_ACCEPTED":
                    contractOfferAcceptedManager.setData(notifData);
                    contractOfferAcceptedManager.sendNotification();
                    break;
                case "CONTRACT_OFFER_REJECTED":
                    contractOfferRejectedManager.setData(notifData);
                    contractOfferRejectedManager.sendNotification();
                    break;
                case "SUGGESTION_ACCEPTED":
                    suggestionAcceptedManager.setData(notifData);
                    suggestionAcceptedManager.sendNotification();
                    break;
                case "SUGGESTION_REJECTED":
                    suggestionRejectedManager.setData(notifData);
                    suggestionRejectedManager.sendNotification();
                    break;
                case "OTHER":
                    otherManager.setData(notifData);
                    otherManager.sendNotification();
                    break;
                default:
                    System.out.println("Unknown event received: \n" + data);
            }
        } catch (Exception exception) {
            System.out.println("Consumed wrong type of data");
            exception.printStackTrace();
        }
    }
}
