package searchengine.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private StatusType status;
    @Column(name = "status_time", columnDefinition = "DATETIME")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LemmaEntity> lemmaSet = new HashSet<>();
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PageEntity> pageSet = new HashSet<>();

    public SiteEntity(String name, String url, StatusType type) {
        synchronized (this) {
            this.name = name;
            this.status = type;
            this.statusTime = LocalDateTime.now();
            this.url = url;
            this.lastError = "";
        }
    }

    public SiteEntity(String name, String url, StatusType type, String lastError) {
        this.name = name;
        this.status = type;
        this.statusTime = LocalDateTime.now();
        this.url = url;
        this.lastError = lastError;
    }

    public void setTimeNow() {
        statusTime = LocalDateTime.now();
    }

}
