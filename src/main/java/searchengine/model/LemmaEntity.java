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
    public LemmaEntity(SiteEntity site, String lemma) {
        synchronized (this) {
            this.site = site;
            this.lemma = lemma;
            this.frequency = 1;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;
    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity site;
    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private Set<IndexEntity> indexSet = new HashSet<>();

    public static Comparator<LemmaEntity> getFrequencyComparator(){
        return Comparator.comparingInt(LemmaEntity::getFrequency);
    }

}

