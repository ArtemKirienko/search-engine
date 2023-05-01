package searchengine.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "page", uniqueConstraints =
@UniqueConstraint(columnNames = {"path", "site_id"}))
public class Page {


    public Page(Site site, String path, int code, String content) {
        synchronized (this) {

            this.site = site;
            this.path = path;
            this.code = code;
            this.content = content;
        }

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;


    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "page",cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Index> indexSet = new HashSet<>();


}


