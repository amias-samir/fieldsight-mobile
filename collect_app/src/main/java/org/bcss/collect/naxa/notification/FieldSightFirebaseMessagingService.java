package org.bcss.collect.naxa.notification;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import timber.log.Timber;

public class FieldSightFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Timber.i("firebase message Id: %s",remoteMessage.getMessageId());
        Timber.i("firebase message Id: %s",remoteMessage.getMessageId());
    }


}
