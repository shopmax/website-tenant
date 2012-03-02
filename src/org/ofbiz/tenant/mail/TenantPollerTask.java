/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.tenant.mail;

import java.util.TimerTask;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.mail.MimeMessageWrapper;
import org.ofbiz.service.mail.ServiceMcaUtil;

public class TenantPollerTask extends TimerTask {
    
    public final static String module = TenantPollerTask.class.getName();
    
    public static final String INBOX = "INBOX";

    protected Store store;
    protected Session session;
    protected long maxSize = 1000000;
    protected boolean deleteMail = false;
    LocalDispatcher dispatcher;
    GenericValue userLogin;

    public TenantPollerTask(Store store, Session session, long maxSize, LocalDispatcher dispatcher, GenericValue userLogin) {
        this.store = store;
        this.session = session;
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
    }

    @Override
    public void run() {
        if (UtilValidate.isNotEmpty(store)) {
            try {
                checkMessages(store, session);
            } catch (Exception e) {
                // Catch all exceptions so the loop will continue running
                Debug.logError("Mail service invocation error for mail store " + store + ": " + e, module);
            }
            if (store.isConnected()) {
                try {
                    store.close();
                } catch (Exception e) {}
            }
        }
    }

    protected void checkMessages(Store store, Session session) throws MessagingException {
        if (!store.isConnected()) {
            store.connect();
        }

        // open the default folder
        Folder folder = store.getDefaultFolder();
        if (!folder.exists()) {
            throw new MessagingException("No default (root) folder available");
        }

        // open the inbox
        folder = folder.getFolder(INBOX);
        if (!folder.exists()) {
            throw new MessagingException("No INBOX folder available");
        }

        // get the message count; stop if nothing to do
        folder.open(Folder.READ_WRITE);
        int totalMessages = folder.getMessageCount();
        if (totalMessages == 0) {
            folder.close(false);
            return;
        }

        // get all messages
        Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.ENVELOPE);
        profile.add(FetchProfile.Item.FLAGS);
        profile.add("X-Mailer");
        folder.fetch(messages, profile);

        // process each message
        for (Message message: messages) {
            // process each un-read message
            if (!message.isSet(Flags.Flag.SEEN)) {
                long messageSize = message.getSize();
                if (message instanceof MimeMessage && messageSize >= maxSize) {
                    Debug.logWarning("Message from: " + message.getFrom()[0] + "not received, too big, size:" + messageSize + " cannot be more than " + maxSize + " bytes", module);

                    // set the message as read so it doesn't continue to try to process; but don't delete it
                    message.setFlag(Flags.Flag.SEEN, true);
                } else {
                    this.processMessage(message, session);
                    if (Debug.verboseOn()) Debug.logVerbose("Message from " + UtilMisc.toListArray(message.getFrom()) + " with subject [" + message.getSubject() + "]  has been processed." , module);
                    message.setFlag(Flags.Flag.SEEN, true);
                    if (Debug.verboseOn()) Debug.logVerbose("Message [" + message.getSubject() + "] is marked seen", module);

                    // delete the message after processing
                    if (deleteMail) {
                        if (Debug.verboseOn()) Debug.logVerbose("Message [" + message.getSubject() + "] is being deleted", module);
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                }
            }
        }

        // expunge and close the folder
        folder.close(true);
    }

    protected void processMessage(Message message, Session session) {
        if (message instanceof MimeMessage) {
            MimeMessageWrapper wrapper = new MimeMessageWrapper(session, (MimeMessage) message);
            try {
                ServiceMcaUtil.evalRules(dispatcher, wrapper, userLogin);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem processing message", module);
            }
        }
    }
}
