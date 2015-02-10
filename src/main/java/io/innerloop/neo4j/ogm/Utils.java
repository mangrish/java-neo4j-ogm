package io.innerloop.neo4j.ogm;

import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Utils
{

    public static int size(Iterable<?> iterable)
    {
        return (iterable instanceof Collection) ? ((Collection<?>) iterable).size() : size(iterable.iterator());
    }

    public static int size(Iterator<?> iterator)
    {
        int count = 0;
        while (iterator.hasNext())
        {
            iterator.next();
            count++;
        }
        return count;
    }

    public static boolean isEmpty(String string)
    {
        return string == null || string.length() == 0;
    }

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

    public static boolean isNotEmpty(String string)
    {
        return string != null && string.length() > 0;
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
