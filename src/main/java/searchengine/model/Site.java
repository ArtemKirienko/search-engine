package searchengine.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "site")
public class Site {




    public Site( String name, String url, StatusType type) {


        this.name = name;
        this.status = type;
        setTimeNow();
        this.url = url;
        this.lastError = "";
    }
    public Site( String name, String url, StatusType type, String lastError) {


        this.name = name;
        this.status = type;
        setTimeNow();
        this.url = url;
        this.lastError = lastError;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private StatusType status;


    @Column(name = "status_time", columnDefinition = "DATETIME")
    private String statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;



    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lemma> lemmaSet = new HashSet<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Page> pageSet = new HashSet<>();

    public void setTimeNow() {

       // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        statusTime = formatter.format(LocalDateTime.now()).toString();
    }
    //public static LocalDateTimeNow
    @Transient
  final public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
