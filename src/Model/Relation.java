package Model;

/**
 * Created by The Illsionist on 2018/8/17.
 */
public class Relation<M,R> {

    private M first;
    private M second;
    private R rel;

    public Relation(M first, M second, R rel){
        this.first = first;
        this.second = second;
        this.rel = rel;
    }

    public M getFirst(){
        return first;
    }

    public M getSecond(){
        return second;
    }

    public R getRel(){
        return rel;
    }

}
