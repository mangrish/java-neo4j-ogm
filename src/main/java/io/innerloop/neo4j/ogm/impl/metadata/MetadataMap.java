package io.innerloop.neo4j.ogm.impl.metadata;

import io.innerloop.neo4j.ogm.annotations.Transient;
import io.innerloop.neo4j.ogm.impl.index.Index;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by markangrish on 28/01/2015.
 */
public class MetadataMap
{
    private static final Logger LOG = LoggerFactory.getLogger(MetadataMap.class);

    public static boolean isInnerClass(Class<?> clazz)
    {
        return clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers());
    }

    private Map<Class<?>, ClassMetadata> lookupByClass;

    private Map<String, ClassMetadata<?>> lookupByLabel;

    private Map<NodeLabel, ClassMetadata<?>> lookupBySortedMultiLabel;

    public MetadataMap(String... packages)
    {
        this.lookupByClass = new HashMap<>();
        this.lookupByLabel = new HashMap<>();
        this.lookupBySortedMultiLabel = new HashMap<>();

        Reflections reflections = new Reflections(packages, new SubTypesScanner(false));

        List<Class<?>> classesToProcess = new ArrayList<>();
        List<Class<?>> interfacesToProcess = new ArrayList<>();

        for (String type : reflections.getAllTypes())
        {
            try
            {
                Class<?> aClass = Class.forName(type);

                if (aClass.isInterface())
                {
                    interfacesToProcess.add(aClass);
                }
                else if (aClass.isAnnotationPresent(Transient.class) || aClass.isAnnotation() ||
                         aClass.isEnum() || isInnerClass(aClass) || aClass.isMemberClass() ||
                         aClass.isAnonymousClass() ||
                         aClass.isLocalClass() ||
                         Throwable.class.isAssignableFrom(aClass))
                {
                    LOG.info("Ignoring class from OGM: [{}]", aClass.getName());
                }
                else
                {
                    classesToProcess.add(aClass);
                }
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new RuntimeException("Could not load class: [" + type + "]. See chained exception for details.",
                                           cnfe);
            }
        }

        for (Class<?> cls : classesToProcess)
        {
            LOG.debug("Adding class to OGM: [{}]", cls.getSimpleName());

            List<String> labels = new ArrayList<>();

            String primaryLabel = cls.getSimpleName();
            labels.add(primaryLabel);

            Class<?> superClass = cls.getSuperclass();

            while (superClass != null && !superClass.getName().equals("java.lang.Object"))
            {
                Class<?>[] interfaces = cls.getInterfaces();

                for (Class<?> interfaceCls : interfaces)
                {
                    if (interfaceCls.isInterface() && interfacesToProcess.contains(interfaceCls))
                    {
                        labels.add(primaryLabel);
                    }
                }
                labels.add(superClass.getSimpleName());
                superClass = superClass.getSuperclass();
            }

            String[] labelArray = labels.toArray(new String[labels.size()]);
            NodeLabel key = new NodeLabel(labelArray);
            ClassMetadata<?> classMetadata = new ClassMetadata<>(cls, classesToProcess, primaryLabel, key);

            lookupByLabel.put(primaryLabel, classMetadata);
            lookupByClass.put(cls, classMetadata);
            lookupBySortedMultiLabel.put(key, classMetadata);
        }
    }

    public ClassMetadata get(String label)
    {
        return lookupByLabel.get(label);
    }

    public ClassMetadata get(NodeLabel nodeLabel)
    {
        return lookupBySortedMultiLabel.get(nodeLabel);
    }

    public <T> ClassMetadata<T> get(Class<T> type)
    {
        return lookupByClass.get(type);
    }

    public <T> ClassMetadata<T> get(T entity)
    {
        return lookupByClass.get(entity.getClass());
    }

    public Collection<Index> getIndexes()
    {
        Collection<Index> indexes = new ArrayList<>();
        for (ClassMetadata classMetadata : lookupByClass.values())
        {
            indexes.addAll(classMetadata.getIndexes());
        }

        return indexes;
    }
}
