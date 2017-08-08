/**
 * La presente clase tiene como objetivo simular un par no-ordenado. No es de mi autoria, sin embargo, se modifico para
 * poder 'marcar' a los pares distinguibles.
 * @author Adam Kapelner
 * @version 1.0
 * @since 08/08/207
 * @link https://github.com/kapelner/bartMachine/blob/master/bartMachine/java/OpenSourceExtensions/UnorderedPair.java
 */
public class UnorderedPair<E extends Comparable<E>> implements Comparable<UnorderedPair<E>> {
    private final E first;
    private final E second;
    private boolean distinguishable;

    /**
     * Creates an unordered pair of the specified elements. The order of the arguments is irrelevant,
     * so the first argument is not guaranteed to be returned by {@link #getFirst()}, for example.
     * @param a one element of the pair. Must not be <tt>null</tt>.
     * @param b one element of the pair. Must not be <tt>null</tt>. May be the same as <tt>a</tt>.
     */
    public UnorderedPair(E a, E b) {
        if (a.compareTo(b) < 0) {
            this.first = a;
            this.second = b;
        } else {
            this.first = b;
            this.second = a;
        }
    }

    /**
     * Gets the smallest element of the pair (according to its {@link Comparable} implementation).
     * @return an element of the pair. <tt>null</tt> is never returned.
     */
    public E getFirst() {
        return first;
    }

    /**
     * Gets the largest element of the pair (according to its {@link Comparable} implementation).
     * @return an element of the pair. <tt>null</tt> is never returned.
     */
    public E getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + 173 * second.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnorderedPair<?> other = (UnorderedPair<?>) obj;
        if (!first.equals(other.first))
            return false;
        if (!second.equals(other.second))
            return false;
        return true;
    }

    public int compareTo(UnorderedPair<E> o) {
        int firstCmp = first.compareTo(o.first);
        if (firstCmp != 0)
            return firstCmp;
        return second.compareTo(o.second);
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }

    public boolean isDistinguishable() {
        return distinguishable;
    }

    public void setDistinguishable(boolean distinguishable) {
        this.distinguishable = distinguishable;
    }
}