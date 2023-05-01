package searchengine.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import searchengine.config.ExceedingNumberPages;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lemma", uniqueConstraints =
@UniqueConstraint(columnNames ={ "lemma","site_id"}))
public class Lemma {


public Lemma (Site site, String lemma, int frequency, int countPage) throws ExceedingNumberPages {
    this.site = site;
    this.lemma = lemma;

    if(frequency > countPage ){
        throw new ExceedingNumberPages("превышение допустимого количества страниц");
    }
    this.frequency = frequency;
}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;


    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;



    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private Set<Index> indexSet = new HashSet<>();

    public synchronized void setFrequency(int frequency) {
        this.frequency = frequency;
    }

}

