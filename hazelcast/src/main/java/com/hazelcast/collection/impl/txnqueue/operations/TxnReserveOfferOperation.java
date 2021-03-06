/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.collection.impl.txnqueue.operations;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.collection.impl.queue.operations.QueueBackupAwareOperation;
import com.hazelcast.collection.impl.queue.QueueContainer;
import com.hazelcast.collection.impl.queue.QueueDataSerializerHook;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.WaitNotifyKey;
import com.hazelcast.spi.WaitSupport;

import java.io.IOException;

/**
 * Reserve offer operation for the transactional queue.
 */
public class TxnReserveOfferOperation extends QueueBackupAwareOperation implements WaitSupport {

    private int txSize;
    private String transactionId;

    public TxnReserveOfferOperation() {
    }

    public TxnReserveOfferOperation(String name, long timeoutMillis, int txSize, String transactionId) {
        super(name, timeoutMillis);
        this.txSize = txSize;
        this.transactionId = transactionId;
    }

    @Override
    public void run() throws Exception {
        QueueContainer queueContainer = getOrCreateContainer();
        if (queueContainer.hasEnoughCapacity(txSize + 1)) {
            response = queueContainer.txnOfferReserve(transactionId);
        }
    }

    @Override
    public WaitNotifyKey getWaitKey() {
        QueueContainer queueContainer = getOrCreateContainer();
        return queueContainer.getOfferWaitNotifyKey();
    }

    @Override
    public boolean shouldWait() {
        QueueContainer queueContainer = getOrCreateContainer();
        return getWaitTimeout() != 0 && !queueContainer.hasEnoughCapacity(txSize + 1);
    }

    @Override
    public void onWaitExpire() {
        getResponseHandler().sendResponse(null);
    }

    @Override
    public boolean shouldBackup() {
        return response != null;
    }

    @Override
    public Operation getBackupOperation() {
        return new TxnReserveOfferBackupOperation(name, (Long) response, transactionId);
    }

    @Override
    public int getId() {
        return QueueDataSerializerHook.TXN_RESERVE_OFFER;
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeInt(txSize);
        out.writeUTF(transactionId);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        txSize = in.readInt();
        transactionId = in.readUTF();
    }
}
