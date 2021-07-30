package aero.icarus2020.consumers;

import aero.icarus2020.exceptions.MissingAttributeException;
import aero.icarus2020.models.EmailNotification;
import aero.icarus2020.services.AsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Service
public class EmailKafkaListener {

    @Autowired
    private AsyncService service;

    @KafkaListener(topics = "${kafka.topic.email}")
    public void receive(@Payload EmailNotification data, @Headers MessageHeaders headers) throws ExecutionException, InterruptedException, IOException, MissingAttributeException {

        System.out.println("Event " + data.getEventType() + " is being processed.");
        String body = null, to = data.getTo();

        if (data.getEventType() == null) throw new MissingAttributeException("eventType");
        switch (data.getEventType()) {
            case "DATA_CHECKIN_JOB_COMPLETED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else
                    body = "Congratulations! Your organization’s data check-in job " + data.getContent().get("assetName") + " has successfully completed and its related data asset " + data.getContent().get("assetName") + " is now available in the ICARUS platform.\n" +
                            "For any changes you may need to its description, please visit your Asset Portfolio in the ICARUS Platform.";
                break;
            case "DATA_CHECKIN_JOB_FAILED":
                if (!data.getContent().containsKey("dataCheckinJobName"))
                    throw new MissingAttributeException("dataCheckinJobName");
                else
                    body = "Unfortunately, your organization’s data check-in job " + data.getContent().get("dataCheckinJobName") + " has failed.\n" +
                            "You may visit the ICARUS On-Premise Worker for details on what went wrong. Please report the issue, if applicable, and try again."; //(if you created this data check-in job).";
                break;
            case "DATASET_ADDED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("orgName")) throw new MissingAttributeException("orgName");
                else
                    body = "A new data asset entitled " + data.getContent().get("assetName") + " has been just added by " + data.getContent().get("orgName") + " in the ICARUS Platform.\n" +
                            "You may search for it in the Marketplace to find out more details!";
                break;
            case "DATA_ASSET_UPDATE_COMPLETED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else
                    body = "Congratulations! The data asset entitled " + data.getContent().get("assetName") + " has been updated successfully.";
                break;
            case "DATA_ASSET_UPDATE_FAILED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else
                    body = "The update of the data asset entitled " + data.getContent().get("assetName") + " has failed.\n" +
                            "You may visit the ICARUS On-Premise Worker for details on what went wrong.\n" +
                            "Please report the issue, if applicable, and try again";
                break;
            case "INFORM_DATASET_UPDATED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName"))
                    throw new MissingAttributeException("senderName");
                else
                    body = "The data asset entitled " + data.getContent().get("assetName") + " has been updated by " + data.getContent().get("senderName") + ".\n" +
                            "You may visit your Asset Portfolio in the ICARUS Platform to retrieve the latest version.";
                break;
            case "DATASET_DOWNLOAD_COMPLETED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("downloadLink"))
                    throw new MissingAttributeException("downloadLink");
                else if (!data.getContent().containsKey("expirationTimestamp"))
                    throw new MissingAttributeException("expirationTimestamp");
                else
                    body = "The data asset entitled " + data.getContent().get("assetName") + " is now available to be downloaded on: " + data.getContent().get("downloadLink") + ".\n" +
                            "Please keep in mind that this link will be valid till: " + data.getContent().get("expirationTimestamp") + ".";
                break;
            case "DATASET_DOWNLOAD_FAILED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else
                    body = "Unfortunately, the download for the data asset entitled " + data.getContent().get("assetName") + " has failed.";
                break;
            case "JOB_STARTED":
                if (!data.getContent().containsKey("jobName")) throw new MissingAttributeException("jobName");
                else
                    body = "The analytics job " + data.getContent().get("jobName") + " which you have created in your Workspace in the ICARUS Platform, has started.\n" +
                            "Please stay tuned for progress updates!";
                break;
            case "JOB_COMPLETED":
                if (!data.getContent().containsKey("jobName")) throw new MissingAttributeException("jobName");
                else
                    body = "The analytics job " + data.getContent().get("jobName") + " which you have created, has completed successfully.\n" +
                            "You may visit your Workspace in the ICARUS Platform to access and visualize the results.";
                break;
            case "JOB_FAILED":
                if (!data.getContent().containsKey("jobName")) throw new MissingAttributeException("jobName");
                else
                    body = "Unfortunately, the analytics job " + data.getContent().get("jobName") + " which you have created, has failed.\n" +
                            "You may check the job details in your Workspace in the ICARUS Platform to understand what went wrong and try again.";
                break;
            case "REQUEST":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName"))
                    throw new MissingAttributeException("senderName");
                else
                    body = "A new request for acquiring the data asset entitled " + data.getContent().get("assetName") + " was made by the organization: " + data.getContent().get("senderName") + ".\n" +
                            "Please visit the ICARUS Platform and check its details in the Contracts in your Asset Portfolio to take appropriate action.";
                break;
            case "REQUEST_REJECTED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName"))
                    throw new MissingAttributeException("senderName");
                else
                    body = "Unfortunately, your request to buy the data asset entitled " + data.getContent().get("assetName") + " was rejected by its provider, " + data.getContent().get("senderName") + ".";
                break;
            case "CONTRACT_OFFERED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName"))
                    throw new MissingAttributeException("senderName");
                else
                    body = "A draft contract for the data asset entitled " + data.getContent().get("assetName") + " has been prepared by " + data.getContent().get("senderName") + " and it is now available for your review. \n" +
                            "Please visit the ICARUS Platform and check the details in the Contracts in your Asset Portfolio to take appropriate action.";
                break;
            case "CONTRACT_ACCEPTED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName"))
                    throw new MissingAttributeException("senderName");
                else
                    body = "Congratulations! Your contract for the data asset entitled " + data.getContent().get("assetName") + " was accepted by " + data.getContent().get("senderName") + ".\n" +
                            "Once you settle the associated payment, you will obtain access to the data asset in the ICARUS Platform.";
                break;
            case "CONTRACT_REJECTED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName"))
                    throw new MissingAttributeException("senderName");
                else
                    body = "Unfortunately, your contract for the data asset entitled " + data.getContent().get("assetName") + " was rejected by " + data.getContent().get("senderName") + ".";
                break;
            case "CONTRACT_PAID":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName")) throw new MissingAttributeException("senderName");
                else
                    body = "Your contract for the data asset entitled " + data.getContent().get("assetName") + " was marked as paid by its provider " + data.getContent().get("senderName") + ", and you now have access to the data asset as agreed.\n" +
                            "You may visit your Asset Portfolio in the ICARUS Platform to download the contract terms and start using the data asset.";
                break;
            case "CONTRACT_NEGOTIATED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else
                    body = "The contract for the data asset entitled " + data.getContent().get("assetName") + " is under negotiation. Please visit the ICARUS Platform and check the details in the Contracts under your Asset Portfolio in order to take appropriate action.";
                break;
            case "CONTRACT_COUNTERED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName")) throw new MissingAttributeException("senderName");
                else
                    body = "Your contract for the data asset entitled " + data.getContent().get("assetName") + " has been revised by its provider " + data.getContent().get("senderName") + ". Please visit the ICARUS Platform and check the details in the Contracts under your Asset Portfolio in order to take appropriate action.";
                break;
            case "CONTRACT_OFFER_REJECTED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName")) throw new MissingAttributeException("senderName");
                else
                    body = "Unfortunately, the draft contract for the data asset entitled " + data.getContent().get("assetName") + " has been rejected by " + data.getContent().get("senderName") + ".";
                break;
            case "CONTRACT_OFFER_ACCEPTED":
                if (!data.getContent().containsKey("assetName")) throw new MissingAttributeException("assetName");
                else if (!data.getContent().containsKey("senderName")) throw new MissingAttributeException("senderName");
                else
                    body = "The draft contract for the data asset entitled " + data.getContent().get("assetName") + " has been accepted by " + data.getContent().get("senderName") + ".";
                break;
            case "SUGGESTION_ACCEPTED":
                if (!data.getContent().containsKey("title")) throw new MissingAttributeException("title");
                else
                    body = "Your suggestion for a new concept entitled \"" + data.getContent().get("title") + "\" has been accepted and it is now included in the ICARUS aviation data model.\n" +
                            "You may now visit the ICARUS Platform to start using it.";
                break;
            case "SUGGESTION_REJECTED":
                if (!data.getContent().containsKey("title")) throw new MissingAttributeException("title");
                else
                    body = "Unfortunately, your suggestion for a new concept \"" + data.getContent().get("title") + "\" to be added in the ICARUS aviation data model has been rejected. \n" +
                            "The typical reasons for this decision are that the specific concept is already included in the model with a synonym term or that it was considered as out of scope at the moment.";
                break;
            case "OTHER":
                if (!data.getContent().containsKey("targetType")) throw new MissingAttributeException("targetType");
                else if (!data.getContent().containsKey("targetId")) throw new MissingAttributeException("targetId");
                else if (!data.getContent().containsKey("body")) throw new MissingAttributeException("body");
                else
                    body = "You have the following notification regarding the " + data.getContent().get("targetType") + " with id: " + data.getContent().get("targetId") + ". " + data.getContent().get("body") + ".";
                break;
            default:
                System.out.println("Unknown event received: \n" + data);
        }

        if (body != null) service.sendMail(to, data.getSubject(), body);
    }

    public AsyncService getService() {
        return service;
    }
}
