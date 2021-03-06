package org.kish.entity;

import lombok.Getter;
import lombok.Setter;
import org.kish.utils.Utils;

import java.util.ArrayList;

/**
 *  게시글의 본문과 같은 내용을 포함하고 있는 클래스입니다.
 *  DataBase를 상속받아 save() 메소드를 사용할경우 post/posts/{메뉴ID}/{postID}.json 에 저장됩니다.
 *  또한 postKey는 {메뉴ID},{postID}의 형태로서 95,28 같은 String입니다.
 */
@SuppressWarnings("unchecked")
@Getter
@Setter
public class Post{
    private String title, author, content, post_date, url;
    private int menu, id;
    private long last_updated;
    private boolean hasAttachments;
    private ArrayList<Attachment> attachments = new ArrayList<>();

    public Post(){}

    public Post(int menu, int id){
        this.menu = menu;
        this.id = id;
        this.setUrl();
    }

    public boolean hasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean v){
        this.hasAttachments = v;
    }

    public void setMenu(int menu) {
        this.menu = menu;
        this.setUrl();
    }

    public void setId(int id) {
        this.id = id;
        this.setUrl();
    }

    public void setUrl(){
        this.url = Utils.postUrlGenerator(this.getMenu(), this.getId());
    }

    public void setHasAttachments(int i){
        this.hasAttachments = i > 0;
    }

    public void addAttachment(String name, String url) {
        this.attachments.add(new Attachment(name, url));
    }

    public void setAttachments(ArrayList<Attachment> list) {
        this.attachments = list;
    }

    @Getter
    public class Attachment {
        private String name;
        private String url;

        public Attachment(String name, String url) {
           this.name = name;
           this.url = url;
        }
    }
}
