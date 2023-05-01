package searchengine.repository;

import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConf;
import searchengine.model.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Repository
@Transactional(isolation = Isolation.SERIALIZABLE)
public class Repo {


    @PersistenceContext
    private EntityManager entityManager;



    public synchronized void removePage(Page page) {
        page = entityManager.find(Page.class, page.getId());
        entityManager.remove(page);
    }

    public synchronized void creatSite(Site site) {

        entityManager.persist(site);
    }

    public synchronized int countPages(Site site) {

        List<Integer> list = entityManager.createQuery(" from Page  where  site_id = :id")
                .setParameter("id", site.getId())
                .getResultList();

        if (list.isEmpty()) {
            return 0;
        }
        return list.size();
    }

    public synchronized int countLemma(Site site) {

        List<Integer> list = entityManager.createQuery(" from Lemma  where  site_id = :id")
                .setParameter("id", site.getId())
                .getResultList();

        if (list.isEmpty()) {
            return 0;
        }
        return list.size();
    }

    public synchronized void creatEnPage(Page pageEn) {
        entityManager.persist(pageEn);
    }




    public void delEnSite(String url) {
        System.out.println("проверка делита");
        List<Site> lsite =
                entityManager.createQuery(" from Site  WHERE url = : url")
                        .setParameter("url", url)
                        .getResultList();

        if (!lsite.isEmpty()) {
            entityManager.remove(lsite.get(0));
        }

    }

    public List<Site> getAllSite() {
        List<Site> list = entityManager.createQuery("from Site ").getResultList();
        return list;
    }




    public void insertLemmas(String lemmas) {
        try {
            entityManager.unwrap(Session.class)
                    .doWork(new Work() {

                        @Override
                        public void execute(Connection con) throws SQLException {

                            PreparedStatement stmt = con.prepareStatement("insert into lemma (frequency, lemma, site_id) values " + lemmas + " ON DUPLICATE KEY UPDATE frequency=frequency+1;");
                            stmt.execute();

                        }
                    });
        } catch (Exception e) {
            insertLemmas(lemmas);

        }

    }

    public void insertIndexes(String indexes) {

        try {
            entityManager.unwrap(Session.class)
                    .doWork(new Work() {

                        @Override
                        public void execute(Connection con) throws SQLException {

                            PreparedStatement stmt = con.prepareStatement("insert into in_dex (ra_nk, lemma_id, page_id) VALUES " + indexes + " ;");

                            stmt.execute();

                        }
                    });
        } catch (Exception e) {
            insertIndexes(indexes);

        }

    }



    public synchronized List<Lemma> selectLemmas(String lemmaKeys) {
        try {
            List<Lemma> list =
                    entityManager.createQuery(" FROM Lemma  WHERE lemma IN " + lemmaKeys + " ").getResultList();

            return list;
        } catch (Exception e) {

            return selectLemmas(lemmaKeys);

        }

    }



    public List<Site> findEntSite(String str) {
        List<Site> list =
                entityManager.createQuery(" FROM Site  WHERE url = :str")
                        .setParameter("str", str)
                        .getResultList();

        return list;
    }

    public void updateShortPage(Page entPage) {
        entityManager.unwrap(Session.class).update(entPage);
    }

    public void updateShortSite(Site entSite) {
        entityManager.unwrap(Session.class).update(entSite);
    }



    public Set<Lemma> findMaxFreqLem(int param) {
        List<Integer> freq =

                entityManager.createQuery("select frequency from Lemma order by frequency desc").getResultList();
        if (freq.isEmpty()) {
            return new HashSet<>();
        }
        List<Lemma> lLemma =
                entityManager.createQuery("from Lemma where frequency >= :freq - " + param + " order by frequency asc ")
                        .setParameter("freq", freq.get(0))
                        .getResultList();
        if (lLemma.isEmpty()) {
            return new HashSet<>();
        }
        Set<Lemma> lemmaSet = new HashSet<>(lLemma);

        return lemmaSet;

    }

    public List<Page> getPageLike( Set<String> lemmaSet, String siteUrl) {
        if(lemmaSet.isEmpty()){
            return new ArrayList<>();
        }

        Iterator<String> iter = lemmaSet.iterator();
        List<Page> page = new ArrayList<>();
        int i = 0;
        List<Index> indexList ;
        while(iter.hasNext()){

            String value = iter.next();


            if(i == 0){

                 indexList = entityManager.createQuery("from Index where lemma_id = (select id from Lemma where lemma = :value and site_id = (select id from Site where url = :url))")
                         .setParameter("value", value)
                         .setParameter("url", siteUrl)
                         .getResultList();
                 page = indexList.stream().map(in -> in.getPage()).collect(Collectors.toList());

            }
            if(i > 0){
                page.stream().filter(p -> p.getIndexSet().stream().anyMatch(is-> is.getLemma().equals(value)) ).collect(Collectors.toList());
            }
            i++;
        }
        return page;
    }

}