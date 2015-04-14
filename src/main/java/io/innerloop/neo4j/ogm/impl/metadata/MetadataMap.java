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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

    private Map<NodeLabel, ClassMetadata<?>> lookupByNodeLabel;

    private Reflections reflections;

    public MetadataMap(String... packages)
    {
        reflections = new Reflections(packages, new SubTypesScanner(false));

        this.lookupByClass = new HashMap<>();
        this.lookupByNodeLabel = new HashMap<>();

        List<Class<?>> classesToProcess = new ArrayList<>();
        List<Class<?>> interfacesToProcess = new ArrayList<>();

        for (String type : reflections.getAllTypes())
        {
            try
            {
                Class<?> aClass = Class.forName(type);

                if (aClass.isInterface())
                {
                    LOG.trace("Marking interface as Processable: [{}]", aClass.getName());
                    interfacesToProcess.add(aClass);
                }
                else if (aClass.isAnnotationPresent(Transient.class) || aClass.isAnnotation() ||
                         aClass.isEnum() || isInnerClass(aClass) || aClass.isMemberClass() ||
                         aClass.isAnonymousClass() ||
                         aClass.isLocalClass() ||
                         Throwable.class.isAssignableFrom(aClass))
                {
                    LOG.debug("Ignoring class from OGM: [{}]", aClass.getName());
                }
                else
                {
                    LOG.trace("Marking class as Processable: [{}]", aClass.getName());
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
            Set<String> labels = new LinkedHashSet<>();

            String primaryLabel = cls.getSimpleName();
            labels.add(primaryLabel);

            Class<?> superClass = cls.getSuperclass();

            while (superClass != null && !superClass.getName().equals("java.lang.Object"))
            {
                if (!classesToProcess.contains(superClass))
                {
                    throw new RuntimeException("Superclass of [" + cls.getName() + "]: [" + superClass.getName() +
                                               "] is not in managed packages.");
                }
                labels.add(superClass.getSimpleName());
                addInterfaceLabels(superClass, labels, interfacesToProcess);
                superClass = superClass.getSuperclass();
            }

            addInterfaceLabels(cls, labels, interfacesToProcess);

            String[] labelArray = labels.toArray(new String[labels.size()]);
            NodeLabel key = new NodeLabel(labelArray);

            if (!cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()))
            {
                ClassMetadata<?> classMetadata = new ClassMetadata<>(cls, classesToProcess, primaryLabel, key);
                lookupByClass.put(cls, classMetadata);
                lookupByNodeLabel.put(key, classMetadata);
            }
        }
    }


    private void addInterfaceLabels(Class<?> cls, Set<String> labels, List<Class<?>> interfacesToProcess)
    {
        Class<?>[] interfaces = cls.getInterfaces();
        for (Class<?> interfaceCls : interfaces)
        {
            if (interfaceCls.isInterface() && interfacesToProcess.contains(interfaceCls))
            {
                labels.add(interfaceCls.getSimpleName());
            }
        }
    }

    public Set<? extends Class<?>> findSubTypesOf(Class<?> type)
    {
        return reflections.getSubTypesOf(type);
    }

    public ClassMetadata get(NodeLabel nodeLabel)
    {
        return lookupByNodeLabel.get(nodeLabel);
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
