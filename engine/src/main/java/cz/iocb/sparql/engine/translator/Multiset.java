package cz.iocb.sparql.engine.translator;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;



public final class Multiset<E> implements Collection<E>, Serializable
{
    private static final class MutableInt implements Serializable
    {
        private static final long serialVersionUID = 1L;

        int value;


        @Override
        public boolean equals(Object o)
        {
            if(o == this)
                return true;

            if(!(o instanceof MutableInt other))
                return false;

            return value == other.value;
        }


        @Override
        public int hashCode()
        {
            return Integer.hashCode(value);
        }
    }


    private static final long serialVersionUID = 1L;

    private final HashMap<E, MutableInt> map = new HashMap<>();
    private int size = 0;


    public Multiset()
    {
    }


    public Multiset(Collection<E> elements)
    {
        addAll(elements);
    }


    public static <E> Multiset<E> copyOf(Iterable<? extends E> it)
    {
        Multiset<E> ms = new Multiset<E>();
        ms.addAll(it);

        return ms;
    }


    @Override
    public int size()
    {
        return size;
    }


    public int distinctSize()
    {
        return map.size();
    }


    @Override
    public boolean add(E element)
    {
        MutableInt box = map.computeIfAbsent(element, _ -> new MutableInt());

        box.value++;
        size++;

        return true;
    }


    public boolean addAll(Iterable<? extends E> it)
    {
        boolean changed = false;

        for(E e : it)
            changed |= add(e);

        return changed;
    }


    public int count(Object element)
    {
        MutableInt box = map.get(element);

        return box == null ? 0 : box.value;
    }


    @Override
    public boolean remove(Object element)
    {
        MutableInt box = map.get(element);

        if(box == null)
            return false;

        if(box.value == 1)
        {
            size -= box.value;
            map.remove(element);
        }
        else
        {
            box.value--;
            size--;
        }

        return true;
    }


    @Override
    public boolean contains(Object element)
    {
        return count(element) > 0;
    }


    @Override
    public void clear()
    {
        map.clear();
        size = 0;
    }


    @Override
    public Iterator<E> iterator()
    {
        return new Iterator<E>()
        {
            private final Iterator<Map.Entry<E, MutableInt>> entryIt = map.entrySet().iterator();
            private E currentElement;
            private MutableInt currentBox;
            private int remainingInBox = 0;
            private boolean canRemove = false;

            @Override
            public boolean hasNext()
            {
                return remainingInBox > 0 || entryIt.hasNext();
            }

            @Override
            public E next()
            {
                if(remainingInBox == 0)
                {
                    Map.Entry<E, MutableInt> e = entryIt.next();
                    currentElement = e.getKey();
                    currentBox = e.getValue();
                    remainingInBox = currentBox.value;
                }

                remainingInBox--;
                canRemove = true;

                return currentElement;
            }

            @Override
            public void remove()
            {
                if(!canRemove)
                    throw new IllegalStateException();

                if(currentBox.value <= 0)
                    throw new IllegalStateException("inconsistent state");

                canRemove = false;
                currentBox.value--;
                size--;

                if(currentBox.value == 0)
                    entryIt.remove();
            }
        };
    }


    @Override
    public void forEach(Consumer<? super E> action)
    {
        Objects.requireNonNull(action);

        for(Map.Entry<E, MutableInt> e : map.entrySet())
            for(int i = 0; i < e.getValue().value; i++)
                action.accept(e.getKey());
    }


    @Override
    public boolean equals(Object o)
    {
        if(o == this)
            return true;

        if(o instanceof Multiset<?> other)
            return this.mapEquals(other.map);

        if(o instanceof Collection<?> coll)
            return this.mapEquals(Multiset.copyOf(coll).map);

        return false;
    }


    @Override
    public int hashCode()
    {
        int h = 0;

        for(Map.Entry<E, MutableInt> e : map.entrySet())
            h += Objects.hash(e.getKey(), e.getValue().value);

        return h;
    }


    @Override
    public String toString()
    {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        sb.append('[');

        for(Map.Entry<E, MutableInt> e : map.entrySet())
        {
            if(!first)
                sb.append(", ");

            first = false;
            sb.append(e.getKey()).append(" x ").append(e.getValue().value);
        }

        sb.append(']');

        return sb.toString();
    }


    private boolean mapEquals(HashMap<?, MutableInt> other)
    {
        if(this.map.size() != other.size())
            return false;

        for(Map.Entry<E, MutableInt> e : map.entrySet())
        {
            MutableInt o = other.get(e.getKey());

            if(o == null || o.value != e.getValue().value)
                return false;
        }

        return true;
    }


    @Override
    public boolean addAll(Collection<? extends E> elements)
    {
        boolean changed = false;

        for(E e : elements)
            changed |= add(e);

        return changed;
    }


    @Override
    public boolean containsAll(Collection<?> elements)
    {
        boolean result = true;

        for(Object e : elements)
            result &= contains(e);

        return result;
    }


    @Override
    public boolean isEmpty()
    {
        return size == 0;
    }


    @Override
    public boolean removeAll(Collection<?> elements)
    {
        boolean changed = false;

        for(Object e : elements)
            changed |= remove(e);

        return changed;
    }


    @Override
    public boolean retainAll(Collection<?> elements)
    {
        boolean modified = false;

        if(elements == this)
            return false;

        if(isEmpty())
            return false;

        if(elements.isEmpty())
        {
            clear();
            return true;
        }

        Collection<?> c = elements;

        if(!(elements instanceof Set<?>))
            c = new HashSet<>(elements);

        for(Iterator<E> it = iterator(); it.hasNext();)
        {
            E e = it.next();

            if(!c.contains(e))
            {
                it.remove();
                modified = true;
            }
        }

        return modified;
    }


    @Override
    public Object[] toArray()
    {
        Object[] array = new Object[size];

        int i = 0;

        for(Object e : this)
            array[i++] = e;

        return array;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a)
    {
        T[] array = (a.length >= size) ? a : (T[]) Array.newInstance(a.getClass().getComponentType(), size);

        int i = 0;

        for(Object e : this)
            array[i++] = (T) e;

        if(array.length > size)
            array[size] = null;

        return array;
    }
}
