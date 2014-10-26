package com.jayway.jsonpath.internal;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidModificationException;
import com.jayway.jsonpath.spi.json.JsonProvider;

import java.util.Collection;

public abstract class PathRef implements Comparable<PathRef>  {

    public static final PathRef NO_OP = new PathRef(null){
        @Override
        public Object getAccessor() {
            return null;
        }

        @Override
        public void set(Object newVal, Configuration configuration) {}

        @Override
        public void delete(Configuration configuration) {}

        @Override
        public void add(Object newVal, Configuration configuration) {}

        @Override
        public void put(String key, Object newVal, Configuration configuration) {}
    };

    protected Object parent;


    private PathRef(Object parent) {
        this.parent = parent;
    }

    abstract Object getAccessor();

    public abstract void set(Object newVal, Configuration configuration);

    public abstract void delete(Configuration configuration);

    public abstract void add(Object newVal, Configuration configuration);

    public abstract void put(String key, Object newVal, Configuration configuration);

    @Override
    public int compareTo(PathRef o) {
        return this.getAccessor().toString().compareTo(o.getAccessor().toString()) * -1;
    }

    public static PathRef create(Object obj, String property){
        return new ObjectPropertyPathRef(obj, property);
    }

    public static PathRef create(Object obj, Collection<String> properties){
        return new ObjectMultiPropertyPathRef(obj, properties);
    }

    public static PathRef create(Object array, int index){
        return new ArrayIndexPathRef(array, index);
    }

    public static PathRef createRoot(Object root){
        return new RootPathRef(root);
    }

    private static class RootPathRef extends PathRef {

        private RootPathRef(Object parent) {
            super(parent);
        }

        @Override
        Object getAccessor() {
            return "$";
        }

        @Override
        public void set(Object newVal, Configuration configuration) {
            throw new InvalidModificationException("Invalid delete operation");
        }

        @Override
        public void delete(Configuration configuration) {
            throw new InvalidModificationException("Invalid delete operation");
        }

        @Override
        public void add(Object newVal, Configuration configuration) {
            if(configuration.jsonProvider().isArray(parent)){
                configuration.jsonProvider().setProperty(parent, null, newVal);
            } else {
                throw new InvalidModificationException("Invalid add operation. $ is not an array");
            }
        }

        @Override
        public void put(String key, Object newVal, Configuration configuration) {
            if(configuration.jsonProvider().isMap(parent)){
                configuration.jsonProvider().setProperty(parent, key, newVal);
            } else {
                throw new InvalidModificationException("Invalid put operation. $ is not a map");
            }
        }
    }
    private static class ArrayIndexPathRef extends PathRef {

        private int index;

        private ArrayIndexPathRef(Object parent, int index) {
            super(parent);
            this.index = index;
        }

        public void set(Object newVal, Configuration configuration){
            configuration.jsonProvider().setArrayIndex(parent, index, newVal);
        }

        public void delete(Configuration configuration){
            configuration.jsonProvider().removeProperty(parent, index);
        }

        public void add(Object value, Configuration configuration){
            Object target = configuration.jsonProvider().getArrayIndex(parent, index);
            if(target == JsonProvider.UNDEFINED || target == null){
                return;
            }
            if(configuration.jsonProvider().isArray(target)){
                configuration.jsonProvider().setProperty(target, null, value);
            } else {
                throw new InvalidModificationException("Can only add to an array");
            }
        }

        public void put(String key, Object value, Configuration configuration){
            Object target = configuration.jsonProvider().getArrayIndex(parent, index);
            if(target == JsonProvider.UNDEFINED || target == null){
                return;
            }
            if(configuration.jsonProvider().isMap(target)){
                configuration.jsonProvider().setProperty(target, key, value);
            } else {
                throw new InvalidModificationException("Can only add properties to a map");
            }
        }

        @Override
        public Object getAccessor() {
            return index;
        }
    }



    private static class ObjectPropertyPathRef extends PathRef {

        private String property;

        private ObjectPropertyPathRef(Object parent, String property) {
            super(parent);
            this.property = property;
        }

        public void set(Object newVal, Configuration configuration){
            configuration.jsonProvider().setProperty(parent, property, newVal);
        }

        public void delete(Configuration configuration){
            configuration.jsonProvider().removeProperty(parent, property);
        }

        public void add(Object value, Configuration configuration){
            Object target = configuration.jsonProvider().getMapValue(parent, property);
            if(target == JsonProvider.UNDEFINED || target == null){
                return;
            }
            if(configuration.jsonProvider().isArray(target)){
                configuration.jsonProvider().setProperty(target, null, value);
            } else {
                throw new InvalidModificationException("Can only add to an array");
            }
        }

        public void put(String key, Object value, Configuration configuration){
            Object target = configuration.jsonProvider().getMapValue(parent, property);
            if(target == JsonProvider.UNDEFINED || target == null){
                return;
            }
            if(configuration.jsonProvider().isMap(target)){
                configuration.jsonProvider().setProperty(target, key, value);
            } else {
                throw new InvalidModificationException("Can only add properties to a map");
            }
        }

        @Override
        public Object getAccessor() {
            return property;
        }
    }

    private static class ObjectMultiPropertyPathRef extends PathRef {

        private Collection<String> properties;

        private ObjectMultiPropertyPathRef(Object parent, Collection<String> properties) {
            super(parent);
            this.properties = properties;
        }

        public void set(Object newVal, Configuration configuration){
            for (String property : properties) {
                configuration.jsonProvider().setProperty(parent, property, newVal);
            }
        }

        public void delete(Configuration configuration){
            for (String property : properties) {
                configuration.jsonProvider().removeProperty(parent, property);
            }
        }

        @Override
        public void add(Object newVal, Configuration configuration) {
            throw new InvalidModificationException("Add can not be performed to multiple properties");
        }

        @Override
        public void put(String key, Object newVal, Configuration configuration) {
            throw new InvalidModificationException("Add can not be performed to multiple properties");
        }

        @Override
        public Object getAccessor() {
            return Utils.join("&&", properties);
        }
    }
}