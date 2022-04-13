package org.kish.web;

import com.google.gson.Gson;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.MenuID;
import org.kish.database.PostDAO;
import org.kish.entity.Menu;
import org.kish.entity.Noti;
import org.kish.entity.Post;
import org.kish.entity.SimplePost;
import org.kish.utils.Utils;
import org.kish.utils.parser.KishWebParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("unchecked")
@Controller
@RequestMapping("/api/post")
public class PostApiController {
    public boolean checkingNewPostLock = false;

    private static final Gson gson = new Gson();
    private final KishServer main;
    @Autowired
    private PostDAO postDao;

    public PostApiController(KishServer main){
        this.main = main;
    }

    public void checkNewPost(){
        if(checkingNewPostLock) return;
        int postCount = 0;

        for(MenuID menuId : MenuID.values()) {
            String id = menuId.id;
            ArrayList<SimplePost> list = KishWebParser.parseMenu(id, "1");

            if (list.size() < 1) break;
            for (SimplePost sp : list) {
                if(this.postDao.isExistPost(sp.getMenu(), sp.getId())) break;
                Post post = getPostFromServer(sp.getMenu(), sp.getId());
                HashMap<String, String> data = new HashMap<>();

                data.put("type", "newPost");
                data.put("menu", Integer.toString(post.getMenu()));
                data.put("id", Integer.toString(post.getId()));
                data.put("url", post.getUrl());

                String menuName;
                try {
                    MenuID menu = Utils.getMenuFromID(Integer.toString(post.getMenu()));
                    menuName = menu.name;
                } catch(IllegalArgumentException e){
                    menuName = "알 수 없음";
                    MainLogger.error(e);
                }

                if (!KishServer.silent) {
                    Noti noti
                            = new Noti("newKishPostNoti"
                            , post.getTitle()
                            , "새 글이 올라왔습니다.\n메뉴 : " + menuName + "\n작성자 : " + post.getAuthor());
                    noti.setData(data);
                    this.main.getFirebaseManager().sendFcmWithTopic(noti);
                }

                postCount++;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    MainLogger.error(e);
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                MainLogger.error(e);
            }
        }

        MainLogger.info("새로운 " + postCount + "개의 게시물이 추가되었습니다.");
    }

    public Post getPostFromServer(int menu, int id){
        MainLogger.info("서버에서 가져오는 중 : " + menu + "," + id);

        boolean isNew = !this.postDao.isExistPost(menu, id);
        Post post = KishWebParser.parsePost(menu, id);

        if(post == null){
            if(!isNew) this.postDao.removePost(menu, id);
            return null;
        }

        if(isNew){
            this.postDao.insertPost(post);
        }else{
            this.postDao.updatePost(post);
        }

        return post;
    }

    @RequestMapping("/getMenuIds")
    public @ResponseBody String getMenuIdsApi(){
        JSONObject jsonObject = new JSONObject();
        for(MenuID menu : MenuID.values()){
            jsonObject.put(menu.name(), menu.id);
        }
        return jsonObject.toJSONString();
    }

    @RequestMapping("/getLastUpdatedMenuList")
    public @ResponseBody String getLastUpdatedMenuListApi() {
        ArrayList<Menu> result = new ArrayList<>();
        for (Integer lastUpdatedMenuId : postDao.getLastUpdatedMenu()) {
            String id = Integer.toString(lastUpdatedMenuId);
            for (MenuID value : MenuID.values()) {
                if (value.id.equals(id)) {
                    result.add(new Menu(lastUpdatedMenuId, value.name));
                    break;
                }
            }
        }

        return gson.toJson(result);
    }

    @RequestMapping("/getOrderedMenuIdList")
    public @ResponseBody String getOrderedMenuIdListApi() {
        return gson.toJson(postDao.getLastUpdatedMenu());
    }

    @RequestMapping("/getMenuTitle")
    public @ResponseBody String getMenuTitleApi(@RequestParam int menuId) {
        String id = Integer.toString(menuId);
        return Utils.getMenuTitle(id);
    }

    @RequestMapping("/getPostListHomeSummary")
    public @ResponseBody String getPostListHomeSummary() {
        ArrayList<Object> result = new ArrayList<>();

        LinkedHashMap<String, Object> latestPostListMap = new LinkedHashMap<>();
        latestPostListMap.put("title", "최신 글");
        latestPostListMap.put("menu", "11501150");      // 11501150은 최신 글 메뉴를 명시하는
        latestPostListMap.put("posts", postDao.getLatestPosts(0));
        result.add(latestPostListMap);

        for (Integer lastUpdatedMenu : postDao.getLastUpdatedMenu()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            ArrayList<SimplePost> postList = new ArrayList<>();

            for (Post post : postDao.getPostsByMenu(lastUpdatedMenu, 0)) {
                postList.add(new SimplePost(post));
            }
            map.put("title", Utils.getMenuTitle(lastUpdatedMenu.toString()));
            map.put("menu", lastUpdatedMenu);
            map.put("posts", postList);

            result.add(map);
        }

        return gson.toJson(result);
    }

    @RequestMapping("/getPostsByMenu")
    public @ResponseBody String getPostsByMenuApi(@RequestParam int menu, @RequestParam(required = false, defaultValue = "0") int page){
        if (menu == 11501150) return this.getLatestPostsApi(page);
        return gson.toJson(Utils.convertPostList2SimplePostList(postDao.getPostsByMenu(menu, page)));
    }

    @RequestMapping("/getLatestPosts")
    public @ResponseBody String getLatestPostsApi(@RequestParam(required = false, defaultValue = "0") int page) {
        return gson.toJson(Utils.convertPostList2SimplePostList(postDao.getLatestPosts(page)));
    }

    @RequestMapping("/getPostContentHtml")
    public @ResponseBody String getPostContentHtmlApi(@RequestParam int menu, @RequestParam int id) {
        try {
            String contentHtml = KishWebParser.generatePostToNormal(Jsoup.connect("http://hanoischool.net/default.asp?board_mode=view&menu_no=" + menu + "&bno=" + id).get());
            Document doc = Jsoup.parse(contentHtml);
            Utils.downloadImgs(doc);
            Utils.replaceImgPaths(doc, "../../../resource/downloaded/");
            Utils.enhanceImgTags(doc);
            contentHtml = doc.html();

            return "<head>" +
                    "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                    "<link href=\"https://fonts.googleapis.com/css2?family=Nanum+Gothic&display=swap\" rel=\"stylesheet\">" +
                    "<style> body { font-family: 'Nanum Gothic', sans-serif;} </style>" +
                    "</head><body>" + contentHtml + "</body>";
        } catch (IOException e) {
            MainLogger.error(e);
            return "죄송합니다. 서버 처리 중 오류가 발생하였습니다.";
        }
    }

    @RequestMapping("/getPostAttachments")
    public @ResponseBody String getPostAttachments(@RequestParam int menu, @RequestParam int id) {
        return gson.toJson(KishWebParser.parsePost(menu, id).getAttachments());
    }

    @RequestMapping("/searchPost")
    public @ResponseBody String searchPostApi(@RequestParam String keyword, @RequestParam int index){
        MainLogger.info("검색요청 : " + keyword);
        /*ArrayList<PostInfo> result = new ArrayList<>();
        Utils.search(this.postInKeyword, this.postInfo, keyword, index).forEach(postKey -> {
            PostInfo postInfo = this.getPostInfo(postKey);
            if(postInfo == null) return;
            result.add(postInfo);
        });
        return gson.toJson(result);*/
        List<Post> foundPosts = this.postDao.searchPost(keyword, index);
        ArrayList<SimplePost> searchResult = new ArrayList<>();

        for (Post post : foundPosts) {
            SimplePost simplePost = new SimplePost(post);
            searchResult.add(simplePost);
        }
        return gson.toJson(searchResult);
    }

    @RequestMapping(value = "/getPost")
    public @ResponseBody String getPostApi(@RequestParam int menu, @RequestParam int id){
        Post post = this.postDao.selectPost(menu, id);
        if(post == null) post = this.getPostFromServer(menu, id);
        if(post == null) return "{ rpc: 404 }";

        return (gson.toJson(post));
    }

    /**
     * <p>검색기능을 위해 학교 홈페이지 내 모든 게시글을 불러옵니다</p>
     * <p>(최초 실행시 필요할 수 있음)</p>
     */
    public void parseAllPosts(){
        MainLogger.warn("모든 게시물 조회를 시작합니다. 상당한 시간이 소요될 수 있습니다.");
        Thread thread = new Thread( () -> {
            int cnt = 0;
            int postCount = 0;
            int totalMenuCount =  MenuID.values().length;

            for(MenuID menuId : MenuID.values()){
                MainLogger.info((cnt++) + " / " + totalMenuCount);
                String id = menuId.id;
                int i = 1;

                while(true) {
                    ArrayList<SimplePost> list = KishWebParser.parseMenu(id, Integer.toString(i));
                    if(list.size() < 1) break;
                    for (SimplePost sp : list){
                        if(this.postDao.isExistPost(sp.getMenu(), sp.getId())){
                            MainLogger.warn(sp.getMenu() + "," + sp.getId() + "가 이미 저장되어있습니다. skip...");
                            continue;
                        }

                        this.getPostFromServer(sp.getMenu(), sp.getId());
                        postCount++;

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            MainLogger.error(e);
                        }
                    }
                    i++;
                }
            }
            MainLogger.info("총 " + postCount + "개의 게시물이 저장되었습니다.");
            checkingNewPostLock = false;
        });
        thread.start();
    }
}
