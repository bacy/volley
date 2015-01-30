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

import java.util.List;

import com.android.volley.db.DbException;
import com.android.volley.db.table.ColumnUtils;
import com.android.volley.db.table.Foreign;
import com.android.volley.db.table.Table;

public class ForeignLazyLoader<T> {
    private final Foreign foreignColumn;
    private Object columnValue;

    public ForeignLazyLoader(Foreign foreignColumn, Object value) {
        this.foreignColumn = foreignColumn;
        this.columnValue = ColumnUtils.convert2DbColumnValueIfNeeded(value);
    }

    public List<T> getAllFromDb() throws DbException {
        List<T> entities = null;
        Table table = foreignColumn.getTable();
        if (table != null) {
            entities = table.db.findAll(
                    Selector.from(foreignColumn.getForeignEntityType()).
                            where(foreignColumn.getForeignColumnName(), "=", columnValue)
            );
        }
        return entities;
    }

    public T getFirstFromDb() throws DbException {
        T entity = null;
        Table table = foreignColumn.getTable();
        if (table != null) {
            entity = table.db.findFirst(
                    Selector.from(foreignColumn.getForeignEntityType()).
                            where(foreignColumn.getForeignColumnName(), "=", columnValue)
            );
        }
        return entity;
    }

    public void setColumnValue(Object value) {
        this.columnValue = ColumnUtils.convert2DbColumnValueIfNeeded(value);
    }

    public Object getColumnValue() {
        return columnValue;
    }
}
