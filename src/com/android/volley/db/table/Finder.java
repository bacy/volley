package com.android.volley.db.table;

import java.lang.reflect.Field;
import java.util.List;

import android.database.Cursor;

import com.android.volley.VolleyLog;
import com.android.volley.db.DbException;
import com.android.volley.db.sqlite.ColumnDbType;
import com.android.volley.db.sqlite.FinderLazyLoader;

/**
 * Author: wyouflf
 * Date: 13-9-10
 * Time: 下午7:43
 */
public class Finder extends Column {

    private final String valueColumnName;
    private final String targetColumnName;

    /* package */ Finder(Class<?> entityType, Field field) {
        super(entityType, field);

        com.android.volley.db.annotation.Finder finder =
                field.getAnnotation(com.android.volley.db.annotation.Finder.class);
        this.valueColumnName = finder.valueColumn();
        this.targetColumnName = finder.targetColumn();
    }

    public Class<?> getTargetEntityType() {
        return ColumnUtils.getFinderTargetEntityType(this);
    }

    public String getTargetColumnName() {
        return targetColumnName;
    }

    @Override
    public void setValue2Entity(Object entity, Cursor cursor, int index) {
        Object value = null;
        Class<?> columnType = columnField.getType();
        Object finderValue = TableUtils.getColumnOrId(entity.getClass(), this.valueColumnName).getColumnValue(entity);
        if (columnType.equals(FinderLazyLoader.class)) {
            value = new FinderLazyLoader(this, finderValue);
        } else if (columnType.equals(List.class)) {
            try {
                value = new FinderLazyLoader(this, finderValue).getAllFromDb();
            } catch (DbException e) {
                VolleyLog.e(e.getMessage(), e);
            }
        } else {
            try {
                value = new FinderLazyLoader(this, finderValue).getFirstFromDb();
            } catch (DbException e) {
                VolleyLog.e(e.getMessage(), e);
            }
        }

        if (setMethod != null) {
            try {
                setMethod.invoke(entity, value);
            } catch (Throwable e) {
                VolleyLog.e(e.getMessage(), e);
            }
        } else {
            try {
                this.columnField.setAccessible(true);
                this.columnField.set(entity, value);
            } catch (Throwable e) {
                VolleyLog.e(e.getMessage(), e);
            }
        }
    }

    @Override
    public Object getColumnValue(Object entity) {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public ColumnDbType getColumnDbType() {
        return ColumnDbType.TEXT;
    }
}
