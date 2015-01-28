package io.innerloop.neo4j.ogm.mapping;

import io.innerloop.neo4j.client.Graph;
import io.innerloop.neo4j.client.Node;
import io.innerloop.neo4j.client.Relationship;
import io.innerloop.neo4j.client.json.JSONObject;
import io.innerloop.neo4j.ogm.annotations.Entity;
import io.innerloop.neo4j.ogm.metadata.SortedMultiLabel;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by markangrish on 19/12/2014.
 */
public class ObjectMapper
{
//    private static Map<String, NodeMetaData> nodesByPrimaryLabel = new HashMap<>();
//
//    private static Map<SortedMultiLabel, NodeMetaData> nodesByOrderedLabels = new HashMap<>();
//
//    private static Map<Class, Converter> converters = new HashMap<>();
//
//    private static void setField(Class<?> clazz, String fieldName, Object value, Object object)
//    {
//        if (value == null)
//        {
//            return;
//        }
//
//        try
//        {
//            Field field = clazz.getDeclaredField(fieldName);
//            field.setAccessible(true);
//            Class<?> type = field.getType();
//            Object convertedValue = convert(type, value);
//            field.set(object, convertedValue);
//        }
//        catch (NoSuchFieldException | IllegalAccessException e)
//        {
//            Class superClass = clazz.getSuperclass();
//            if (superClass != null)
//            {
//                setField(superClass, fieldName, value, object);
//            }
//        }
//    }
//
//    private static Object convert(Class<?> type, Object value)
//    {
//        if (type.isEnum())
//        {
//            return Enum.valueOf((Class<Enum>) type, (String) value);
//        }
//
//        Converter converter = converters.get(type);
//
//        if (converter != null)
//        {
//            return converter.deserialize(value);
//        }
//
//        return value;
//    }
//
//    private static Object flatten(Class<?> type, Object value)
//    {
//        if (value == null)
//        {
//            return null;
//        }
//        if (type.isEnum())
//        {
//            return value.toString();
//        }
//
//        Converter converter = converters.get(type);
//
//        if (converter != null)
//        {
//            return converter.serialize(value);
//        }
//
//        return value;
//    }
//
//    private static Field getField(Class clazz, String fieldName)
//    {
//        try
//        {
//            return clazz.getDeclaredField(fieldName);
//        }
//        catch (NoSuchFieldException e)
//        {
//            Class superClass = clazz.getSuperclass();
//            if (superClass == null)
//            {
//                return null;
//            }
//            else
//            {
//                return getField(superClass, fieldName);
//            }
//        }
//    }
//
//    public ObjectMapper(String... packages)
//    {
//        Reflections reflections = new Reflections(packages);
//        Set<Class<?>> ns = reflections.getTypesAnnotatedWith(Entity.class);
//
//        for (Class<?> n : ns)
//        {
//            List<String> labels = new ArrayList<>();
//
//            String primaryLabel = n.getSimpleName();
//            labels.add(primaryLabel);
//
//            Class<?> superClass = n.getSuperclass();
//
//            while (superClass != null && !superClass.getName().equals("java.lang.Object"))
//            {
//                labels.add(superClass.getSimpleName());
//                superClass = superClass.getSuperclass();
//            }
//
//            String[] labelArray = labels.toArray(new String[labels.size()]);
//            Arrays.sort(labelArray);
//            SortedMultiLabel key = new SortedMultiLabel(labelArray);
//            NodeMetaData<?> metaData = new NodeMetaData<>(key, n);
//            nodesByPrimaryLabel.put(primaryLabel, metaData);
//            nodesByOrderedLabels.put(key, metaData);
//
//            // need to ensure indexes and constraints are set up.
//
//            register(new CurrencyConverter());
//            register(new LocalDateTimeConverter());
//            register(new ZoneIdConverter());
//            register(new YearMonthConverter());
//            register(new UUIDConverter());
//        }
//
////        Set<Class<?>> rns = reflections.getTypesAnnotatedWith(RelationshipEntity.class);
////
////        for (Class<?> rn : rns)
////        {
////            RelationshipEntity annotation = rn.getAnnotation(RelationshipEntity.class);
////            String type = annotation.type();
////            if (type == null || type.length() == 0)
////            {
////                type = rn.getSimpleName();
////            }
////            relationships.put(type, new RelationshipMetaData<>(rn));
////        }
//    }
//
//    public void register(Converter converter)
//    {
//        try
//        {
//            Type[] actualTypeArguments = ((ParameterizedType) converter.getClass()
//                                                                      .getGenericInterfaces()[0]).getActualTypeArguments();
//            Class<?> aClass = Class.forName(actualTypeArguments[0].getTypeName());
//            converters.put(aClass, converter);
//        }
//        catch (ClassNotFoundException e)
//        {
//            throw new RuntimeException("Converter must implement Converter interface directly. Failed to register converter.");
//        }
//    }
//
//    private Object createInstance(Class cls, Map<String, Object> properties)
//    {
//        try
//        {
//            Object instance = cls.newInstance();
//
//            for (Map.Entry<String, Object> entry : properties.entrySet())
//            {
//                setField(cls, entry.getKey(), entry.getValue(), instance);
//            }
//
//            return instance;
//        }
//        catch (InstantiationException | IllegalAccessException e)
//        {
//            throw new RuntimeException("Could not instantiate class class");
//        }
//    }
//
////    private List<Map<String, Object>> dump(Collection collection)
////    {
////        List<Map<String, Object>> result = new ArrayList<>();
////        if (collection == null)
////        {
////            return result;
////        }
////        for (Object object : collection)
////        {
////            result.save(dump(object));
////        }
////
////        return result;
////    }
//
//    public Object dump(Object object)
//    {
//        if (object == null)
//        {
//            return JSONObject.NULL;
//        }
//
//        NodeMetaData metaData = nodesByPrimaryLabel.get(object.getClass().getSimpleName());
//
//        if (metaData != null)
//        {
//            Map<String, Object> result = new HashMap<>();
//
//            Class<?> cls = metaData.getType();
//
//            while (cls != null)
//            {
//                for (Field f : cls.getDeclaredFields())
//                {
//                    f.setAccessible(true);
//                    if (!Modifier.isTransient(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()) &&
//                        !nodesByPrimaryLabel.containsKey(f.getType().getSimpleName()) &&
//                        !Collection.class.isAssignableFrom(f.getType()))
//                    {
//                        Object convertedValue = null;
//                        try
//                        {
//                            convertedValue = flatten(f.getType(), f.get(object));
//                        }
//                        catch (IllegalAccessException e)
//                        {
//                            throw new RuntimeException("Could not convert value.");
//                        }
//                        result.put(f.getName(), convertedValue);
//                    }
//                }
//                cls = cls.getSuperclass();
//            }
//            return result;
//        }
//
//        return object;
//    }
//
//    /**
//     * Used when a type contains a collection of the same type and only one item is requested.
//     *
//     * @param graph
//     * @param type
//     * @param idFieldName
//     * @param idValue
//     * @param <T>
//     *
//     * @return
//     */
//    private <T> T loadUnique(Graph graph, Class<T> type, String idFieldName, Object idValue)
//    {
//        Collection<T> map = loadAll(type, graph);
//
//        if (map.size() == 0)
//        {
//            return null;
//        }
//
//        try
//        {
//            for (T obj : map)
//            {
//                Field field = obj.getClass().getDeclaredField(idFieldName);
//                field.setAccessible(true);
//                if (field.get(obj).equals(idValue))
//                {
//                    return obj;
//                }
//            }
//        }
//        catch (NoSuchFieldException | IllegalAccessException e)
//        {
//            throw new RuntimeException("Field does not exist");
//        }
//
//        return map.iterator().next();
//    }
//
//    public <T> List<T> loadAll(Class<T> type, Graph graph)
//    {
//        Map<Long, Object> seenObjects = new HashMap<>();
//        List<T> results = new ArrayList<>();
//
//        for (Node node : graph.getNodes())
//        {
//            String[] labels = node.getLabels();
//            Arrays.sort(labels);
//            SortedMultiLabel key = new SortedMultiLabel(labels);
//            Class cls = nodesByOrderedLabels.get(key).getType();
//
//            if (cls == null)
//            {
//                throw new RuntimeException("Missing a class implementation for key: [" + key + "]");
//            }
//
//            Map<String, Object> properties = node.getProperties();
//            Object instance = createInstance(cls, properties);
//            seenObjects.put(node.getId(), instance);
//
//            if (type.isAssignableFrom(instance.getClass()))
//            {
//                results.add((T) instance);
//            }
//
//        }
//        for (Relationship relationship : graph.getRelationships())
//        {
//            Object start = seenObjects.get(relationship.getStartNodeId());
//            Object end = seenObjects.get(relationship.getEndNodeId());
//
//            try
//            {
//                String relationshipType = relationship.getType();
//
//                //                RelationshipMetaData rmd = relationships.get(relationshipType);
//
//                //                if (rmd != null)
//                //                {
//                //                    Map<String, Object> properties = relationship.getProperties();
//                //                    Object instance = createInstance(rmd.getCls(), properties);
//                //                    if (type.isAssignableFrom(instance.getClass()))
//                //                    {
//                //                        results.save((T) instance);
//                //                    }
//                //                    //TODO: still missing part where we set to and from nodes on the relationship entity.
//                //                }
//
//                Field field = getField(start.getClass(), relationshipType);
//
//                if (Collection.class.isAssignableFrom(field.getType()))
//                {
//                    field.setAccessible(true);
//                    Collection collection = (Collection) field.get(start);
//                    if (collection == null)
//                    {
//                        collection = new HashSet<>();
//                        setField(start.getClass(), relationshipType, collection, start);
//                    }
//                    collection.add(end);
//                }
//                else
//                {
//                    setField(start.getClass(), relationshipType, end, start);
//                }
//
//            }
//            catch (IllegalAccessException e)
//            {
//                throw new RuntimeException("No field found on object", e);
//            }
//        }
//        return results;
//    }
//
//    public <T> T load(Class<T> type, Graph graph)
//    {
//        Collection<T> map = loadAll(type, graph);
//
//        if (map.size() == 0)
//        {
//            return null;
//        }
//
//        return map.iterator().next();
//    }
}
