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
public class Index {
    public Index(Page page, Lemma lemma, float rank) {
        synchronized (this) {
            this.page = page;
            this.lemma = lemma;
            this.rank = rank;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id")
    private Page page;
    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;
    @Column(name = "`rank`")
    private Float rank;
}
