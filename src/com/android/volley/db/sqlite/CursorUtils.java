/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.db.sqlite;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.database.Cursor;

import com.android.volley.VolleyLog;
import com.android.volley.db.table.Column;
import com.android.volley.db.table.DbModel;
import com.android.volley.db.table.Finder;
import com.android.volley.db.table.Id;
import com.android.volley.db.table.Table;
import com.android.volley.ext.tools.DbTools;

public class CursorUtils {

    public static <T> T getEntity(final DbTools db, final Cursor cursor, Class<T> entityType, long findCacheSequence) {
        if (db == null || cursor == null) return null;

        EntityTempCache.setSeq(findCacheSequence);
        try {
            Table table = Table.get(db, entityType);
            Id id = table.id;
            String idColumnName = id.getColumnName();
            int idIndex = id.getIndex();
            if (idIndex < 0) {
                idIndex = cursor.getColumnIndex(idColumnName);
            }
            Object idValue = id.getColumnConverter().getFieldValue(cursor, idIndex);
            T entity = EntityTempCache.get(entityType, idValue);
            if (entity == null) {
                entity = entityType.newInstance();
                id.setValue2Entity(entity, cursor, idIndex);
                EntityTempCache.put(entityType, idValue, entity);
            } else {
                return entity;
            }
            int columnCount = cursor.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                String columnName = cursor.getColumnName(i);
                Column column = table.columnMap.get(columnName);
                if (column != null) {
                    column.setValue2Entity(entity, cursor, i);
                }
            }

            // init finder
            for (Finder finder : table.finderMap.values()) {
                finder.setValue2Entity(entity, null, 0);
            }
            return entity;
        } catch (Throwable e) {
            VolleyLog.e(e.getMessage(), e);
        }

        return null;
    }
    
    public static <T> T dbModel2Entity(final DbTools db, DbModel dbModel,Class<?> clazz){
        if(dbModel!=null){
            HashMap<String, String> dataMap = dbModel.getDataMap();
            try {
                @SuppressWarnings("unchecked")
                T  entity = (T) clazz.newInstance();
                for(Entry<String, String> entry : dataMap.entrySet()){
                    String columnKey = entry.getKey();
                    Table table = Table.get(db, clazz);
                    Column column = table.columnMap.get(columnKey);
                    if (column != null) {
                        column.setValue(entity, entry.getValue()==null?null:entry.getValue().toString());
                    }
                }
                return entity;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }

    public static DbModel getDbModel(final Cursor cursor) {
        DbModel result = null;
        if (cursor != null) {
            result = new DbModel();
            int columnCount = cursor.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                result.add(cursor.getColumnName(i), cursor.getString(i));
            }
        }
        return result;
    }

    public static class FindCacheSequence {
        private FindCacheSequence() {
        }

        private static long seq = 0;
        private static final String FOREIGN_LAZY_LOADER_CLASS_NAME = ForeignLazyLoader.class.getName();
        private static final String FINDER_LAZY_LOADER_CLASS_NAME = FinderLazyLoader.class.getName();

        public static long getSeq() {
            String findMethodCaller = Thread.currentThread().getStackTrace()[4].getClassName();
            if (!findMethodCaller.equals(FOREIGN_LAZY_LOADER_CLASS_NAME) && !findMethodCaller.equals(FINDER_LAZY_LOADER_CLASS_NAME)) {
                ++seq;
            }
            return seq;
        }
    }

    private static class EntityTempCache {
        private EntityTempCache() {
        }

        private static final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();

        private static long seq = 0;

        public static <T> void put(Class<T> entityType, Object idValue, Object entity) {
            cache.put(entityType.getName() + "#" + idValue, entity);
        }

        @SuppressWarnings("unchecked")
        public static <T> T get(Class<T> entityType, Object idValue) {
            return (T) cache.get(entityType.getName() + "#" + idValue);
        }

        public static void setSeq(long seq) {
            if (EntityTempCache.seq != seq) {
                cache.clear();
                EntityTempCache.seq = seq;
            }
        }
    }
}
