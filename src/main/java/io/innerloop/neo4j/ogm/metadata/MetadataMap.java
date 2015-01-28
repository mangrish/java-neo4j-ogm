package io.innerloop.neo4j.ogm.metadata;

import io.innerloop.neo4j.ogm.annotations.Transient;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by markangrish on 28/01/2015.
 */
public class MetadataMap
{
    private Map<Class<?>, ClassMetadata> lookupByClass;

    private Map<String, ClassMetadata<?>> lookupByLabel;

    private Map<SortedMultiLabel, ClassMetadata<?>> lookupBySortedMultiLabel;

    public MetadataMap(String... packages)
    {
        this.lookupByClass = new HashMap<>();
        this.lookupByLabel = new HashMap<>();
        this.lookupBySortedMultiLabel = new HashMap<>();

        Reflections reflections = new Reflections(packages, new SubTypesScanner(false));

        List<Class<?>> classesToProcess = new ArrayList<>();

        for (String type : reflections.getAllTypes())
        {
            try
            {
                Class<?> aClass = Class.forName(type);

                if (aClass.isAnnotationPresent(Transient.class) || aClass.isInterface() || aClass.isAnnotation() ||
                    aClass.isEnum())
                {
                    continue;
                }

                classesToProcess.add(aClass);
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new RuntimeException("Could not load class: " + type + ". See chained exception for details.",
                                           cnfe);
            }
        }

        for (Class<?> cls : classesToProcess)
        {
            List<String> labels = new ArrayList<>();

            String primaryLabel = cls.getSimpleName();
            labels.add(primaryLabel);

            Class<?> superClass = cls.getSuperclass();

            while (superClass != null && !superClass.getName().equals("java.lang.Object"))
            {
                labels.add(superClass.getSimpleName());
                superClass = superClass.getSuperclass();
            }

            String[] labelArray = labels.toArray(new String[labels.size()]);
            SortedMultiLabel key = new SortedMultiLabel(labelArray);
            ClassMetadata<?> classMetadata = new ClassMetadata<>(cls, primaryLabel, key);

            for (Field field: cls.getDeclaredFields())
            {
                if (Modifier.isTransient(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
                {
                    continue;
                }

                PropertyMetadata pm = new PropertyMetadata(field.getName(),field.getName(), field.getType(), field);
                classMetadata.addPropertyMetadata(pm);
            }

            lookupByLabel.put(primaryLabel, classMetadata);
            lookupByClass.put(cls, classMetadata);
            lookupBySortedMultiLabel.put(key, classMetadata);
        }
    }

    public ClassMetadata get(String label)
    {
        return lookupByLabel.get(label);
    }

    public ClassMetadata get(SortedMultiLabel sortedMultiLabel)
    {
        return lookupBySortedMultiLabel.get(sortedMultiLabel);
    }

    public <T> ClassMetadata<T> get(Class<T> type)
    {
        return lookupByClass.get(type);
    }

    public <T> ClassMetadata<T> get(T entity)
    {
        return lookupByClass.get(entity.getClass());
    }
}
