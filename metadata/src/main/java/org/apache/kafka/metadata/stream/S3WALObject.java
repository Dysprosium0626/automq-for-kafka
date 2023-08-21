/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.metadata.stream;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.common.metadata.WALObjectRecord;
import org.apache.kafka.server.common.ApiMessageAndVersion;

public class S3WALObject extends S3Object {

    private Integer brokerId;
    private Map<Long/*streamId*/, S3ObjectStreamIndex> streamsIndex;

    private S3ObjectType objectType = S3ObjectType.UNKNOWN;

    public S3WALObject(Long objectId) {
        super(objectId);
    }

    private S3WALObject(
        final Long objectId,
        final Long objectSize,
        final String objectAddress,
        final Long applyTimeInMs,
        final Long expiredTimeImMs,
        final Long commitTimeInMs,
        final Long destroyTimeInMs,
        final S3ObjectState s3ObjectState,
        final S3ObjectType objectType,
        final Integer brokerId,
        final List<S3ObjectStreamIndex> streamsIndex) {
        super(objectId, objectSize, objectAddress, applyTimeInMs, expiredTimeImMs, commitTimeInMs, destroyTimeInMs, s3ObjectState, objectType);
        this.objectType = objectType;
        this.brokerId = brokerId;
        this.streamsIndex = streamsIndex.stream().collect(
            Collectors.toMap(S3ObjectStreamIndex::getStreamId, index -> index));
    }

    @Override
    public void onCreate(S3ObjectCommitContext createContext) {
        super.onCreate(createContext);
        if (!(createContext instanceof WALObjectCommitContext)) {
            throw new IllegalArgumentException();
        }
        WALObjectCommitContext walCreateContext = (WALObjectCommitContext) createContext;
        this.streamsIndex = walCreateContext.streamIndexList.stream().collect(Collectors.toMap(S3ObjectStreamIndex::getStreamId, index -> index));
        this.brokerId = walCreateContext.brokerId;
    }

    class WALObjectCommitContext extends S3ObjectCommitContext {

        private final List<S3ObjectStreamIndex> streamIndexList;
        private final Integer brokerId;

        public WALObjectCommitContext(
            final Long createTimeInMs,
            final Long objectSize,
            final String objectAddress,
            final S3ObjectType objectType,
            final List<S3ObjectStreamIndex> streamIndexList,
            final Integer brokerId) {
            super(createTimeInMs, objectSize, objectAddress, objectType);
            this.streamIndexList = streamIndexList;
            this.brokerId = brokerId;
        }
    }

    public ApiMessageAndVersion toRecord() {
        return new ApiMessageAndVersion(new WALObjectRecord()
            .setObjectId(objectId)
            .setObjectState((byte) s3ObjectState.ordinal())
            .setObjectType((byte) objectType.ordinal())
            .setAppliedTimeInMs(appliedTimeInMs.get())
            .setExpiredTimeInMs(expiredTimeInMs.get())
            .setCommittedTimeInMs(committedTimeInMs.get())
            .setDestroyedTimeInMs(destroyedTimeInMs.get())
            .setObjectSize(objectSize.get())
            .setStreamsIndex(
                streamsIndex.values().stream()
                    .map(S3ObjectStreamIndex::toRecordStreamIndex)
                    .collect(Collectors.toList())), (short) 0);
    }

    public static S3WALObject of(WALObjectRecord record) {
        S3WALObject s3WalObject = new S3WALObject(
            record.objectId(), record.objectSize(), null,
            record.appliedTimeInMs(), record.expiredTimeInMs(), record.committedTimeInMs(), record.destroyedTimeInMs(),
            S3ObjectState.fromByte(record.objectState()), S3ObjectType.fromByte(record.objectType()),
            record.brokerId(), record.streamsIndex().stream().map(S3ObjectStreamIndex::of).collect(Collectors.toList()));
        return s3WalObject;
    }

    public Integer getBrokerId() {
        return brokerId;
    }

    public Map<Long, S3ObjectStreamIndex> getStreamsIndex() {
        return streamsIndex;
    }

    @Override
    public S3ObjectType getObjectType() {
        return objectType;
    }
}
