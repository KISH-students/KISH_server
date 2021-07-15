package org.kish.web;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping("/api/kish-magazine")
public class KishMagazineApiController {
    public static String DIR_NAME = "magazine";
    private static final Gson gson = new Gson();
    private final File libraryResourceDir;
    private final HashMap<String, List<Object>> cachedHome = new HashMap<>();
    private final HashMap<String, List<Object>> hiddenCachedHome = new HashMap<>();
    @Autowired
    private CacheManager cacheManager;


    public KishMagazineApiController(){
        this.libraryResourceDir = new File(KishServer.RESOURCE_PATH.getAbsolutePath() + File.separator + DIR_NAME);
    }

    public boolean clearCache(String name) {
        Cache cache = cacheManager.getCache("name");
        if(cache != null) {
            cache.clear();
            return true;
        } else {
            return false;
        }
    }

    public void updateArticles(){
        boolean isChanged = false;

        clearCache("articleList");
        clearCache("library_parent_list");
        clearCache("library_category_dir");

        File originalArticlesPath = new File("kish magazine");
        if(!originalArticlesPath.exists()) originalArticlesPath.mkdirs();

        ArrayList<File> docs = (ArrayList<File>) Utils.getAllSubFiles(originalArticlesPath);
        for (File article : docs) {
            String pdfFullPath = article.getAbsolutePath();
            String articleExtension = FilenameUtils.getExtension(article.getName());

            pdfFullPath = pdfFullPath
                    .replace(originalArticlesPath.getAbsolutePath(), libraryResourceDir.getAbsolutePath())
                    .replace("." + articleExtension, ".pdf")
                    .replace("&", ", ");

            File pdfFile = new File(pdfFullPath);
            if(pdfFile.exists()) continue;

            isChanged = true;
            File imgFile = new File(pdfFullPath.substring(0, pdfFullPath.length() - 3) + "png");
            try {
                switch (articleExtension) {
                    case "png":
                    case "jpg":
                    case "docx":
                        MainLogger.info("매거진 기사 추가 중 : " + article.getName());
                        KishServer.jodManager.fileToPDF(article, pdfFile);

                        PDDocument document = PDDocument.load(pdfFile);
                        Utils.extractFirstImgFromPdf(document, imgFile);
                        document.close();
                        break;
                    case "pdf":
                        MainLogger.info("매거진 기사 추가 중 : " + article.getName());
                        FileUtils.copyFile(article, pdfFile);

                        PDDocument pdDocument = PDDocument.load(pdfFile);
                        Utils.extractFirstImgFromPdf(pdDocument, imgFile);
                        pdDocument.close();
                        break;

                }
            } catch (IOException e) {
                MainLogger.error(e);
            }
        }

        if (isChanged) {
            this.cachedHome.clear();
            cacheHome(true);
        }
    }

    public void cacheHome(boolean async) {
        if (async) {
            Thread thread = new Thread(() -> cacheHome(false));
            thread.start();
            return;
        }

        MainLogger.warn("KISH MAGAZINE home 캐싱 작업 시작 ...");
        for (Object parent : gson.fromJson(getParentListApi(), ArrayList.class)) {
            for (Object category : gson.fromJson(getCategoryListApi((String) parent), ArrayList.class)) {
                homeApi((String) parent, (String) category, false);
            }
        }
        MainLogger.warn("KISH MAGAZINE home 캐싱 완료.");
    }

    @GetMapping(value = "home")
    @ResponseBody
    public String homeApi(@RequestParam String parent,
                          @RequestParam String category,
                          @RequestParam(required = false, defaultValue = "false") boolean ios) {
        if (category.equals("all")) {
            ArrayList<Object> result = new ArrayList<>();
            for (Object childCategory : gson.fromJson(getCategoryListApi(parent), ArrayList.class)) {
                String key = parent + ":" + childCategory;

                if (cachedHome.containsKey(key)) {
                    result.addAll(cachedHome.get(key));
                }
                if (!ios && hiddenCachedHome.containsKey(key)) {
                    result.addAll(hiddenCachedHome.get(key));
                }
            }

            Collections.shuffle(result);
            return gson.toJson(result);
        }

        String key = parent + ":" + category;

        if (cachedHome.containsKey(key) || hiddenCachedHome.containsKey(key)) {
            ArrayList<Object> result = new ArrayList<>();
            List<Object> cachedHomeResult = cachedHome.get(key);
            List<Object> hiddenCachedHomeResult = hiddenCachedHome.get(key);
            System.out.println("히캐: " + hiddenCachedHome.get(key).size());
            if (cachedHome.get(key) != null) {
                System.out.println("너캐: " + cachedHome.get(key).size());
            }
            if (cachedHomeResult != null) {
                result.addAll(cachedHomeResult);
            }
            if (!ios && hiddenCachedHomeResult != null) {
                result.addAll(hiddenCachedHomeResult);
            }
            Collections.shuffle(result);
            return gson.toJson(result);
        } else {
            ArrayList<File> subFileList = (ArrayList<File>) Utils.getAllSubFiles(
                    new File(this.libraryResourceDir.getPath() + File.separator + parent + File.separator + category));
            boolean shouldHide;

            PDFTextStripper stripper;
            try {
                stripper = new PDFTextStripper();
            } catch (IOException e) {
                MainLogger.error(e);
                return "[]";
            }

            ArrayList<Object> result = new ArrayList<>();
            ArrayList<Object> hiddenResult = new ArrayList<>();
            for (File file : subFileList) {
                String name = file.getName();
                shouldHide = (name.contains("${hidden}") || StringUtils.containsIgnoreCase(name, "covid")
                        || name.contains("코로나")
                        || name.contains("19")
                        || StringUtils.containsIgnoreCase(name, "corona"));
                String extension = FilenameUtils.getExtension(file.getName());
                if (extension.equals("pdf")) {
                    try {
                        PDDocument pdf = PDDocument.load(file);
                        String content;

                        if (!pdf.isEncrypted()) {
                            content = stripper.getText(pdf);
                            if (!shouldHide) {
                                shouldHide = (StringUtils.containsIgnoreCase(content, "covid")
                                        || content.contains("코로나")
                                        || content.contains("19")
                                        || StringUtils.containsIgnoreCase(content, "corona"));
                            }
                        } else {
                            MainLogger.warn(file.getAbsolutePath() + "는 암호화 되어있어 읽을 수 없습니다.");
                            continue;
                        }

                        File imgFile = new File(file.getParentFile().getPath() +
                                File.separator + FilenameUtils.getBaseName(file.getName()) + ".png");

                        Object article;
                        if (content.trim().isEmpty()) {
                            article = new ImgArticle(file);
                        } else if (ThreadLocalRandom.current().nextDouble() < 0.4 && imgFile.exists()) {
                            article = new TextArticleWithImg(file, content);
                        } else {
                            article = new TextArticle(file, content);
                        }

                        if (shouldHide) {
                            hiddenResult.add(article);
                        } else {
                            result.add(article);
                        }

                        pdf.close();
                    } catch (IOException e) {
                        MainLogger.error(e);
                    }
                }
            }

            this.cachedHome.put(key, result);
            this.hiddenCachedHome.put(key, hiddenResult);
            return homeApi(parent, category, ios);
        }
    }

    @Cacheable(value = "library_parent_list")
    @GetMapping(value = "getParentList")
    @ResponseBody
    public String getParentListApi() {
        return gson.toJson(this.libraryResourceDir.list());
    }

    @Cacheable(value = "library_category_dir", key = "#parent")
    @GetMapping(value = "getCategoryList")
    @ResponseBody
    public String getCategoryListApi(@RequestParam String parent) {
        File parentDir = new File(this.libraryResourceDir.getAbsolutePath() + File.separator + parent);
        return gson.toJson(parentDir.list());
    }

    /**
     *  앱 버전 125이하에서 사용하는 API입니다.
     * @param path
     */
    @Cacheable(value = "articleList", key = "#path")
    @GetMapping(value = "getArticleList")
    @ResponseBody
    public String getArticleListApi(@RequestParam(required = false, defaultValue = "") String path){
        ArrayList<Object> rs = new ArrayList<>();
        File file = new File(libraryResourceDir.getAbsolutePath() + File.separator +  path);
        MainLogger.info(libraryResourceDir.getAbsolutePath() + File.separator +  path);
        if(!file.exists() || file.isFile()){
            MainLogger.error(path);
            return "[]";
        }

        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) rs.add(new Category(subFile));
            else if (FilenameUtils.getExtension(subFile.getName()).equals("pdf")) rs.add(new Article(subFile));
        }

        return gson.toJson(rs);
    }


    @Getter
    @Setter
    class Category {
        private final String type = "dir";
        private String name;
        private String path;
        private ArrayList<String> subfileName = new ArrayList<>();

        public Category(File file){
            this.name = file.getName().replace("^", "\n");

            this.path = file.getAbsolutePath().substring(libraryResourceDir.getAbsolutePath().length());
            //this.url = URLEncoder.encode(this.url,"UTF-8");
            //this.url =  file.getAbsolutePath().substring(resourcePath.getAbsolutePath().length());

            for (File subfile : file.listFiles()) {
                if(subfile.isFile()){
                    String fileName = subfile.getName();
                    if(FilenameUtils.getExtension(fileName).equals("pdf"))
                        subfileName.add(FilenameUtils.getBaseName(fileName));
                }
            }
        }
    }

    @Getter
    class Article{
        private final String type = "file";
        private String name;
        private String url;
        private String author = " ";

        public Article(File file){
            String[] tmp = FilenameUtils.getBaseName(file.getName()).replace("^", "\n").split(":");
            this.name = tmp[0];
            if (tmp.length > 1) {
                this.author = tmp[1];
            }

            this.url = "/resource/" + DIR_NAME + file.getAbsolutePath().substring(libraryResourceDir.getAbsolutePath().length());
                /*this.url = URLEncoder
                        .encode(this.url,"UTF-8")
                        .replace("+", "%20")
                        .replace("%2F", "/");
                this.url = "/resource/" + DIR_NAME + this.url;*/
        }
    }

    @Setter
    @Getter
    class ArticleV2 {
        private String url;
        private String title;
        private String author;

        public ArticleV2(File file) {
            String[] tmp = FilenameUtils.getBaseName(file.getName()).replace("^", "\n").split(":");
            this.setTitle(tmp[0]);
            if (tmp.length > 1) {
                this.setAuthor(tmp[1]);
            } else {
                this.setAuthor("");
            }

            this.setUrl("/resource/" + DIR_NAME + file.getAbsolutePath().substring(libraryResourceDir.getAbsolutePath().length()));
        }
    }

    @Setter
    @Getter
    class TextArticle extends ArticleV2 {
        private final String type = "TextArticle";
        private String summary;

        public TextArticle(File file, String content) {
            super(file);
            summary = content;
            summary = summary.trim().replace("\n\n", "").replace("   ", "");
            summary = summary.substring(0, 350) + " ...";
            this.setSummary(summary);
        }
    }

    @Setter
    @Getter
    class TextArticleWithImg extends ArticleV2 {
        private final String type = "TextArticleWithImg";
        private String summary;
        private String img;

        public TextArticleWithImg(File file, String content) {
            super(file);

            summary = content;
            summary = summary.trim().replace("\n\n", "").replace("   ", "");
            summary = summary.substring(0, 200) + " ...";
            this.setSummary(summary);

            String path = file.getAbsolutePath().substring(libraryResourceDir.getAbsolutePath().length());
            path = path.substring(0, path.length() - FilenameUtils.getExtension(path).length()) + "png";
            this.setImg("/resource/" + DIR_NAME + path);
        }
    }

    @Setter
    @Getter
    class ImgArticle extends ArticleV2 {
        private final String type = "ImgArticle";
        private String img;

        public ImgArticle(File file) {
            super(file);

            String path = file.getAbsolutePath().substring(libraryResourceDir.getAbsolutePath().length());
            path = path.substring(0, path.length() - FilenameUtils.getExtension(path).length()) + "png";
            this.setImg("/resource/" + DIR_NAME + path);
        }
    }
}
