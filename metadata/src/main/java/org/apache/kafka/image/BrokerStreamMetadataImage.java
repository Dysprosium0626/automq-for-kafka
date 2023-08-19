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


package org.apache.kafka.image;

import java.util.Objects;
import java.util.Set;
import org.apache.kafka.controller.stream.s3.WALObject;
import org.apache.kafka.image.writer.ImageWriter;
import org.apache.kafka.image.writer.ImageWriterOptions;

public class BrokerStreamMetadataImage {
    private final Integer brokerId;
    private final Set<WALObject> walObjects;

    public BrokerStreamMetadataImage(Integer brokerId, Set<WALObject> walObjects) {
        this.brokerId = brokerId;
        this.walObjects = walObjects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BrokerStreamMetadataImage that = (BrokerStreamMetadataImage) o;
        return Objects.equals(brokerId, that.brokerId) && Objects.equals(walObjects, that.walObjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brokerId, walObjects);
    }

    public void write(ImageWriter writer, ImageWriterOptions options) {
        walObjects.forEach(walObject -> writer.write(walObject.toRecord()));
    }

    public Set<WALObject> getWalObjects() {
        return walObjects;
    }

    public Integer getBrokerId() {
        return brokerId;
    }
}
