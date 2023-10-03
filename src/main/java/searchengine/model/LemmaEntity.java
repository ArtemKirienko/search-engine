package searchengine.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lemma", uniqueConstraints =
@UniqueConstraint(columnNames = {"lemma", "site_id"}))
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteEntity site;
    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private Set<IndexEntity> indexSet = new HashSet<>();

    public LemmaEntity(SiteEntity site, String lemma) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = 1;
    }

    public static Comparator<LemmaEntity> frequencyComparator = (o1, o2) -> {
        int o1freq = o1.getFrequency();
        int o2freq = o2.getFrequency();
        if(o1freq == o2freq){
            return 0;
        }
       return o1freq < o2freq  ? -1 : 1;
    };
}

