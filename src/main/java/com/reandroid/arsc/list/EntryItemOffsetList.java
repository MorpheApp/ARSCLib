/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.arsc.list;

import com.reandroid.arsc.base.BlockCounter;
import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.header.TypeHeader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.arsc.item.OffsetItem;
import com.reandroid.arsc.value.Entry;
import com.reandroid.utils.collection.ArrayCollection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class EntryItemOffsetList extends OffsetReferenceList<OffsetItem> {

    public EntryItemOffsetList(IntegerReference countReference) {
        super(countReference, OffsetItem.CREATOR_OFFSET32);
    }

    @Override
    protected boolean autoFill() {
        return isSparse();
    }

    @Override
    protected void onReadBytes(BlockReader reader) throws IOException {
        int size = getCountReference().get();
        setSize(size);
        if (size == 0) {
            return;
        }
        if (isSparse()) {
            for (int i = 0; i < size; i++) {
                get(i).readBytes(reader);
            }
            return;
        }
        boolean is16 = getCreator() == OffsetItem.CREATOR_OFFSET16;
        int elemSize = is16 ? 2 : 4;
        byte[] buf = new byte[size * elemSize];
        reader.readFully(buf);
        if (is16) {
            int offset = 0;
            for (int i = 0; i < size; i++) {
                int val = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                if (val != OffsetItem.NO_ENTRY16) {
                    OffsetItem item = createAt(i);
                    item.setOffset(val * 4);
                }
                offset += 2;
            }
        } else {
            int offset = 0;
            for (int i = 0; i < size; i++) {
                int val = (buf[offset] & 0xff) |
                        ((buf[offset + 1] & 0xff) << 8) |
                        ((buf[offset + 2] & 0xff) << 16) |
                        ((buf[offset + 3] & 0xff) << 24);
                if (val != OffsetItem.NO_ENTRY) {
                    OffsetItem item = createAt(i);
                    item.setOffset(val);
                }
                offset += 4;
            }
        }
    }

    @Override
    public int readBytes(InputStream inputStream) throws IOException {
        int size = getCountReference().get();
        setSize(size);
        if (size == 0) {
            return 0;
        }
        if (isSparse()) {
            int count = 0;
            for (int i = 0; i < size; i++) {
                count += get(i).readBytes(inputStream);
            }
            return count;
        }
        boolean is16 = getCreator() == OffsetItem.CREATOR_OFFSET16;
        int elemSize = is16 ? 2 : 4;
        byte[] buf = new byte[size * elemSize];
        int total = 0;
        while (total < buf.length) {
            int read = inputStream.read(buf, total, buf.length - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        if (is16) {
            int offset = 0;
            for (int i = 0; i < size; i++) {
                int val = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                if (val != OffsetItem.NO_ENTRY16) {
                    OffsetItem item = createAt(i);
                    item.setOffset(val * 4);
                }
                offset += 2;
            }
        } else {
            int offset = 0;
            for (int i = 0; i < size; i++) {
                int val = (buf[offset] & 0xff) |
                        ((buf[offset + 1] & 0xff) << 8) |
                        ((buf[offset + 2] & 0xff) << 16) |
                        ((buf[offset + 3] & 0xff) << 24);
                if (val != OffsetItem.NO_ENTRY) {
                    OffsetItem item = createAt(i);
                    item.setOffset(val);
                }
                offset += 4;
            }
        }
        return total;
    }

    @Override
    protected int onWriteBytes(OutputStream stream) throws IOException {
        int size = size();
        if (size == 0) {
            return 0;
        }
        if (isSparse()) {
            return super.onWriteBytes(stream);
        }
        int result = 0;
        boolean is16 = getOffsetType() == TypeHeader.OFFSET_16;
        for (int i = 0; i < size; i++) {
            OffsetItem item = get(i);
            if (item != null) {
                result += item.writeBytes(stream);
            } else {
                if (is16) {
                    stream.write(0xff);
                    stream.write(0xff);
                    result += 2;
                } else {
                    stream.write(0xff);
                    stream.write(0xff);
                    stream.write(0xff);
                    stream.write(0xff);
                    result += 4;
                }
            }
        }
        return result;
    }

    @Override
    public int countBytes() {
        if (isSparse()) {
            int result = 0;
            int size = size();
            for (int i = 0; i < size; i++) {
                OffsetItem item = get(i);
                if (item != null) {
                    result += item.countBytes();
                }
            }
            return result;
        }
        return size() * (getOffsetType() == TypeHeader.OFFSET_16 ? 2 : 4);
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(countBytes());
        try {
            writeBytes(stream);
        } catch (IOException ignored) {
        }
        return stream.toByteArray();
    }

    @Override
    public void onCountUpTo(BlockCounter counter) {
        if (counter.FOUND) {
            return;
        }
        counter.setCurrent(this);
        if (counter.END == this) {
            counter.FOUND = true;
            return;
        }
        counter.addCount(countBytes());
    }

    public int getHighestIdx() {
        int result = -1;
        int size = size();
        for (int i = 0; i < size; i++) {
            OffsetItem item = get(i);
            if (item != null) {
                int idx = item.getIdx();
                if (idx > result) {
                    result = idx;
                }
            }
        }
        return result;
    }
    public int indexOfIdx(int idx) {
        int size = size();
        for (int i = 0; i < size; i++) {
            OffsetItem item = get(i);
            if (item != null && idx == item.getIdx()) {
                return i;
            }
        }
        return -1;
    }
    public int findSortPoint(int idx) {
        int size = size();
        for (int i = 0; i < size; i++) {
            OffsetItem item = get(i);
            if (item != null && item.getIdx() > idx) {
                return i;
            }
        }
        return size;
    }
    public boolean isSparse() {
        return getCreator() == OffsetItem.CREATOR_SPARSE;
    }
    public int getOffsetType() {
        Creator<? extends OffsetItem> creator = getCreator();
        if (creator == OffsetItem.CREATOR_OFFSET16) {
            return TypeHeader.OFFSET_16;
        }
        if (creator == OffsetItem.CREATOR_SPARSE) {
            return TypeHeader.OFFSET_SPARSE;
        }
        return TypeHeader.OFFSET_32;
    }
    public boolean setOffsetType(int offsetType, EntryItemList entryItemList) {
        if (offsetType != getOffsetType()) {
            if (offsetType == TypeHeader.OFFSET_SPARSE) {
                changeToSparse(entryItemList);
            } else if (isSparse()) {
                if (offsetType == TypeHeader.OFFSET_16) {
                    changeFromSparse(entryItemList, OffsetItem.CREATOR_OFFSET16);
                } else {
                    changeFromSparse(entryItemList, OffsetItem.CREATOR_OFFSET32);
                }
            } else {
                if (offsetType == TypeHeader.OFFSET_16) {
                    setCreator(OffsetItem.CREATOR_OFFSET16);
                } else {
                    setCreator(OffsetItem.CREATOR_OFFSET32);
                }
                if (!entryItemList.isEmpty()) {
                    clear();
                }
            }
            return !entryItemList.isEmpty();
        }
        return false;
    }
    private void changeFromSparse(EntryItemList entryItemList, Creator<? extends OffsetItem> creator) {
        if (entryItemList.isEmpty()) {
            this.setCreator(creator);
        } else {
            entryItemList.sort();
            List<Integer> idList = listIds(entryItemList);
            int size = idList.size();
            int largest = idList.get(size - 1);
            this.clear();
            this.setCreator(creator);
            int index = 0;
            for (int i = 0; i < size; i++) {
                int id = idList.get(i);
                for (int j = index + 1; j < id; j++) {
                    entryItemList.createAt(j);
                }
                index = id;
            }
            entryItemList.setSize(largest + 1);
            this.setSize(entryItemList.size());
        }
    }
    private void changeToSparse(EntryItemList entryItemList) {
        if (entryItemList.isEmpty()) {
            this.setCreator(OffsetItem.CREATOR_SPARSE);
        } else {
            List<Integer> idList = listIds(entryItemList);
            int size = idList.size();
            this.clear();
            this.setCreator(OffsetItem.CREATOR_SPARSE);
            setSize(size);
            for (int i = 0; i < size; i++) {
                Integer id = idList.get(i);
                OffsetItem offsetItem = getOrCreate(i);
                offsetItem.setIdx(id);
            }
            entryItemList.removeIf(entry -> entry == null || entry.isNull());
        }
    }
    private List<Integer> listIds(EntryItemList entryItemList) {
        int size = entryItemList.size();
        List<Integer> list = new ArrayCollection<>(size);
        for (int i = 0; i < size; i ++) {
            Entry entry = entryItemList.get(i);
            if (entry != null && !entry.isNull()) {
                list.add(entry.getId());
            }
        }
        return list;
    }
}
