package org.kish.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.database.KishDAO;
import org.kish.entity.RequestResult;
import org.kish.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("unchecked")
@Controller
@RequestMapping(value = "/api/library")
public class LibraryApiController {
    public static final Gson gson = new Gson();
    private final KishServer main;
    @Autowired private KishDAO kishDAO;

    public HashMap<String, String> session = new HashMap<>();

    public LibraryApiController(KishServer main){
        this.main = main;
    }

    /**
     * 해당 ID가 이미 존재하는 ID인지 확인합니다
     * @param seq 회원 ID
     * @param id 입력한 ID
     * @param pwd 입력한 비밀번호
     * @param ck 재입력 비밀번호
     * */

    @PostMapping(value = "/checkID")
    public @ResponseBody String checkID(@RequestParam String seq, @RequestParam String id, @RequestParam String pwd,
                                        @RequestParam(required = false, defaultValue = "") String ck){
        String parameters;
        /*ID_EXIST_CHECK 값이 무엇을 의미하는지는 모르겠습니다만, 회원가입 중복확인시 0을 사용합니다 --> 1도 사용되네요..?*/
        parameters = "ID_EXIST_CHECK=" + 0;
        parameters += "&MEMBER_REG_SEQ=" + seq;
        parameters += "&MEMBER_REG_ID=" + id;
        parameters += "&MEMBER_REG_PWD=" + pwd;
        parameters += "&MEMBER_PWD_CK=" + ck;

        /*
         * message : 결과 메세지
         * result가 0이면 사용 가능 아이디
         */
        JSONObject response = WebUtils.postRequestWithJsonResult("http://lib.hanoischool.net:81/front/member/checkID", WebUtils.ContentType.FORM, parameters);
        return response.toJSONString();
    }

    //http://lib.hanoischool.net:81/front/member/register
    @PostMapping(value = "/register")
    public @ResponseBody String register(@RequestParam String uuid, @RequestParam String seq,
                                         @RequestParam String id, @RequestParam String pwd,
                                         @RequestParam String ck){
        String parameters;
        parameters = "ID_EXIST_CHECK=" + 1;
        parameters += "&MEMBER_REG_SEQ=" + seq;
        parameters += "&MEMBER_REG_ID=" + id;
        parameters += "&MEMBER_REG_PWD=" + pwd;
        parameters += "&MEMBER_PWD_CK=" + ck;

        JSONObject response = WebUtils.postRequestWithJsonResult("http://lib.hanoischool.net:81/front/member/register", WebUtils.ContentType.FORM, parameters, this.getCookie(uuid, true));
        return response.toJSONString();
    }

    @PostMapping(value = "/login")
    public @ResponseBody String login(@RequestParam String uuid, @RequestParam(required = false, defaultValue = "") String fcm,
                                      @RequestParam String id, @RequestParam String pwd){
        String parameters;
        parameters = "REQ_URL=";
        parameters += "&MEMBER_ID=" + id;
        parameters += "&MEMBER_PWD=" + pwd;

        RequestResult response = WebUtils.postRequest("http://lib.hanoischool.net:81/front/login", WebUtils.ContentType.FORM, parameters, this.getCookie(uuid, true));
        if (response == null) {
            return serviceTerminated("3");
        }

        if (!fcm.isEmpty()) {   // 신버전
            String seq = "";
            String jsonResponse = response.getResponse();
            Object resultCode = gson.fromJson(jsonResponse, HashMap.class).get("result");
            if ("0".equals(String.valueOf(resultCode))) {
                seq = (String) getInfo(uuid).get("seq");
                kishDAO.registerUser(seq, fcm);
            }

            HashMap map = gson.fromJson(jsonResponse, HashMap.class);
            map.put("seq", seq);
            map.put("admin", kishDAO.isAdmin(seq));
            return gson.toJson(map);
        } else {    // 구버전 대응
            return response.getResponse();
        }
    }

    @PostMapping(value = "/logout")
    public @ResponseBody String logout(@RequestParam String seq, @RequestParam String fcm) {
        kishDAO.unregisterUser(seq, fcm);
        return "";
    }

    @RequestMapping(value = "/getInfo", method = RequestMethod.GET)
    public @ResponseBody JSONObject getInfo(@RequestParam String uuid){
        JSONObject json = new JSONObject();
        if(!this.session.containsKey(uuid)){
            json.put("result", "1");
            json.put("message", "잘못된 요청입니다.");
            return json;
        }

        String response = WebUtils.getRequest("http://lib.hanoischool.net:81/front/mypage/mypage", this.getCookie(uuid)).getResponse();
        if (response == null) {
            //return serviceTerminated("3");
        }

        Document doc = Jsoup.parse(response);
        Elements myPageBox = doc.select(".list-style01");
        if(myPageBox.size() < 2){
            json.put("result", "2");
            json.put("message", "요청이 만료되었습니다.");
            //return json.toJSONString();
        }
        Elements myInfoElements = myPageBox.get(0).select("span");
        Elements loanInfoElements = myPageBox.get(1).select("span");
        json.put("result", "0");
        json.put("seq", myInfoElements.get(0).text());
        json.put("name", myInfoElements.get(1).text());
        json.put("grade", myInfoElements.get(2).text());
        json.put("numberLoanableBooks", myInfoElements.get(3).text());
        json.put("loanRestrictionDate", myInfoElements.get(4).text());

        json.put("numberLoanBooks", loanInfoElements.get(0).text());
        json.put("numberOverdueBooks", loanInfoElements.get(1).text());
        json.put("numberReservedBooks", loanInfoElements.get(2).text());
        return json;
    }

    @RequestMapping(value = "/getLoanedBooks")
    public @ResponseBody String getLoanedBooks(@RequestParam String uuid){
        if(!this.session.containsKey(uuid)){
            return "[-1]";  // 비로그인 상태일경우 index 0에 -1 반환
        }
        JSONArray jsonArray = new JSONArray();
        String response = WebUtils.getRequest("http://lib.hanoischool.net:81/front/mypage/bookLend?SC_BOOKSTATUS_CK=on&SC_BOOKSTATUS_FLAG=0", this.getCookie(uuid)).getResponse();
        if (response == null) {
            return serviceTerminated("3");
        }

        Document doc = Jsoup.parse(response);
        Elements books = doc.select("tbody").get(1).select("tr");
        for(Element book : books){
            Elements infoElements = book.select("td");
            HashMap<String, String> map = new HashMap<>();
            map.put("id", infoElements.get(0).text());
            map.put("registrationNumber", infoElements.get(1).text());
            map.put("billingSymbol", infoElements.get(2).text());
            map.put("bookName", infoElements.get(3).text());
            map.put("loanDate", infoElements.get(4).text());
            map.put("returnScheduledDate", infoElements.get(5).text());
            map.put("returnDate", infoElements.get(6).text());
            jsonArray.add(map);
        }
        return jsonArray.toJSONString();
    }

    /**
     * KISH 학생 여부를 확인합니다
     * @param name 본명
     * @param seq 도서관 회원 ID (대출증 id)*/

    @RequestMapping(value = "/isMember", method = RequestMethod.POST)
    public @ResponseBody String isMember(@RequestParam String name, @RequestParam String seq){
        try {
            name = URLEncoder.encode(name,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            MainLogger.error("name 인코딩중 오류가 발생하였습니다.", e);
            return "{\"message\":\"요청을 처리하는도중 오류가 발생하였습니다.\",\"result\":500}";
        }
        String parameters = "MEMBER_NM=" + name + "&MEMBER_SEQ=" + seq;
        /* result 2 : 이미 가입된 (웹)회원
           result 1 : 존재하지 않는 (도서관)회원
         */
        JSONObject result = WebUtils.postRequestWithJsonResult("http://lib.hanoischool.net:81/front/member/search", WebUtils.ContentType.FORM, parameters);
        return result.toJSONString();
    }

    @RequestMapping(value = "/findID", method = RequestMethod.POST)
    public @ResponseBody String findID(@RequestParam String name, @RequestParam String seq){
        try {
            name = URLEncoder.encode(name,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            MainLogger.error("name 인코딩중 오류가 발생하였습니다.", e);
            return "{\"message\":\"요청을 처리하는도중 오류가 발생하였습니다.\",\"result\":500}";
        }
        String parameters = "FORM_MODE=ID&MEMBER_REG_ID=&MEMBER_NM=" + name + "&MEMBER_SEQ=" + seq;
        // result 0 : 성공, 1 : 아이디 존재 X
        JSONObject result = WebUtils.postRequestWithJsonResult("http://lib.hanoischool.net:81/front/member/searchID", WebUtils.ContentType.FORM, parameters, "");
        return result.toJSONString();
    }

    @RequestMapping(value = "/findPWD", method = RequestMethod.POST)
    public @ResponseBody String findPWD(@RequestParam String id, @RequestParam String name, @RequestParam String seq){
        String parameters = "FORM_MODE=PWD&MEMBER_REG_ID=" + id + "&MEMBER_NM=" + name + "&MEMBER_SEQ=" + seq;
        // result 0 : 성공, 1 : 정보 일치 회원 X
        // RESULT_VALUE에 설정된 임시 pwd 전달됨
        JSONObject result = WebUtils.postRequestWithJsonResult("http://lib.hanoischool.net:81/front/member/searchPWD", WebUtils.ContentType.FORM, parameters, "");
        return result.toJSONString();
    }

    @RequestMapping(value = "/changePWD", method = RequestMethod.POST)
    public @ResponseBody String changePWD(@RequestParam String uuid, @RequestParam String pwd, @RequestParam String ck){
        if(!this.session.containsKey(uuid)){
            return "{\"message\":\"로그인을 해주세요.\",\"result\":3}";
        }
        /* 페이지 비밀번호 규정
        비밀번호는 8~12자 영대문자, 영소문자,
        숫자 및 특수문자 중 2종류 이상으로 구성시 최소 10자리 이상이며,
         3종류 이상 구성시 최소 8자리 이상으로 구성하여야 합니다 . */
        // TODO : 규정에 맞도록 필터링
        if(pwd.length() < 8 || pwd.length() > 12){
            return "{\"message\":\"입력한 비밀번호는 8~12자로 구성되어야 합니다.\",\"result\":4}";
        }
        if(!pwd.equals(ck)){
            return "{\"message\":\"입력한 비밀번호가 다릅니다.\",\"result\":2}";
        }
        String parameters = "USER_PW="+ pwd + "&USER_PW2=" + ck;
        // 아래 결과는 무조건 result값이 0임...
        String result = WebUtils.postRequest("http://lib.hanoischool.net:81/front/member/updatePwd", WebUtils.ContentType.FORM, parameters, this.getCookie(uuid)).getResponse();
        if (result == null) {
            return serviceTerminated("3");
        }
        return "{\"message\":\"성공적으로 비밀번호가 변경되었습니다.\",\"result\":0}";
    }

    @RequestMapping(value = "/searchBooks", method = RequestMethod.GET)
    public @ResponseBody String searchBooks(@RequestParam String keyword, @RequestParam(defaultValue = "1") int index){
        Document doc;
        try {
            doc = Jsoup.connect(
                    "http://lib.hanoischool.net:81/front/bookSearch/simple/list?U_CD=search_menu&M_CD=simpleSearch" +
                            "&PAGE_NO=" + index + "&PAGE_SIZE=12&BLOCK_SIZE=10&BK_BIB_SEQ=&BK_ITEM_SEQ=&ORDER_COLUMN=BK_BIBAUTHOR&ORDER_METHOD=DESC&CHKTYPEALL=ALL&CHKTYPE0=&CHKTYPE2=&CHKTYPE3=&CHKLENDINCLUDE=1&CHKRESERVEINCLUDE=1&SC_LIST_TYPE=list" +
                            "&SC_KEYWORD_FIRST=" + keyword + "&SC_KEY_SECOND=ALL&SC_KEYWORD_SECOND=").get();
        } catch (IOException e) {
            MainLogger.error(e);
            return serviceTerminated("3");
        }

        Elements books = doc.select("tbody").get(0).select("tr");
        JSONObject jsonObject = new JSONObject();
        ArrayList<HashMap<String, String>> bookList = new ArrayList<>();
        for(Element book : books){
            Elements infoElements = book.select("td");
            HashMap<String, String> map = new HashMap<>();
            map.put("id", infoElements.get(0).text());
            map.put("title", infoElements.get(1).text());
            map.put("author", infoElements.get(2).text());
            map.put("publication", infoElements.get(3).text());
            map.put("publishDate", infoElements.get(4).text());
            map.put("loanable", infoElements.get(5).text());
            bookList.add(map);
        }
        Elements tempElements = doc.select(".pagination-sm").get(0).select("li");
        int indexSize = tempElements.size();
        boolean hasNextIndex;
        boolean hasNextBtn = tempElements.select(".fa-angle-right-wrap").size() > 0;
        if(hasNextBtn) indexSize --;
        if(index % 10 == 0){
            hasNextIndex = hasNextBtn;
        }else {
            hasNextIndex = indexSize > (index % 10);
        }
        jsonObject.put("bookList", bookList);
        jsonObject.put("result", "0");
        jsonObject.put("index", index);
        jsonObject.put("hasNextIndex", hasNextIndex);
        return jsonObject.toJSONString();
    }

    public String getCookie(String uuid){
        return getCookie(uuid, false);
    }

    public String getCookie(String uuid, boolean force){
        String cookie;
        if(force || !this.session.containsKey(uuid)) {
            this.session.put(uuid, WebUtils.getNewCookie("http://lib.hanoischool.net:81/"));
        }
        cookie = this.session.get(uuid);
        return cookie;
    }

    public String serviceTerminated(String resultCode) {
        JSONObject json = new JSONObject();
            /*
            도서관 홈페이지는 때때로 오프라인 상태인 경우가 존재합니다.
             */
        json.put("result", resultCode);
        json.put("message", "죄송합니다! \n지금은 도서관 운영시간이 아닙니다.\n나중에 다시 시도해주세요.");
        return json.toJSONString();
    }
}
