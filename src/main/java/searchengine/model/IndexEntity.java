package searchengine.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "`index`")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id")
    private PageEntity page;
    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private LemmaEntity lemma;
    @Column(name = "`rank`")
    private Float rank;

    public IndexEntity(PageEntity page, LemmaEntity lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}
