package io.innerloop.neo4j.ogm.impl.util;

import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by markangrish on 20/02/2015.
 */
public class ReflectionUtils
{

    public static Field getField(Class clazz, String fieldName)
    {
        try
        {
            return clazz.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e)
        {
            Class superClass = clazz.getSuperclass();
            if (superClass == null)
            {
                return null;
            }
            else
            {
                return getField(superClass, fieldName);
            }
        }
    }


    public static void setField(Class<?> clazz, String fieldName, Object value, Object object)
    {
        if (value == null)
        {
            return;
        }

        try
        {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Class<?> type = field.getType();
            //            Object convertedValue = convert(type, value);
            field.set(object, value);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            Class superClass = clazz.getSuperclass();
            if (superClass != null)
            {
                setField(superClass, fieldName, value, object);
            }
        }
    }


    public static Iterable<Field> getFields(Class<?> clazz)
    {

        List<Field> currentClassFields = Lists.newArrayList(clazz.getDeclaredFields());
        Class<?> parentClass = clazz.getSuperclass();

        if (parentClass != null && !(parentClass.equals(Object.class)))
        {
            List<Field> parentClassFields = (List<Field>) getFields(parentClass);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }
}
