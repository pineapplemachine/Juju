/*
 * Juju is Sudoku written in Java! This program and its source code are
 * released as public domain. (But that doesn't mean you should claim credit
 * that isn't yours, or sell it when it could otherwise be had for free, 
 * because that would be a shitty thing of you to do.)
 * Juju was written by Sophie Kirschner. Visit the author's website at
 * http://pineapplemachine.com
 */

// import stuff
package juju;
import java.awt.event.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JApplet;

// main class. has the main loop and all the game logic. run-of-the-mill organization stuff frankly.
public class main extends JApplet implements Runnable, MouseListener, KeyListener{
    // debug stuff, for debugging the code
    boolean debug=false;
    private void debuglog(String message){if(debug){System.out.println(message);}}
    // more squares = more sudoku! warning: key input doesn't also support larger square sizes. oh well.
    final static int squaresize=3,squaresize2=squaresize*squaresize,maxremoved=70,maxcycles=2048;
    // margins (these are minimums, not absolutes)
    final static int xmargin=20,ymargin=20;
    // game stuff
    puzzle puz; // the puzzle object
    int[] playerdata; // contains player-entered numbers
    int[] selection=null; // records the currently selected coord on the sudoku grid
    boolean highlighterrors=true; // let the player know when they've inputted a wrong number?
    LinkedList<int[]> hints=new LinkedList<>(); // list of hinted-at coords
    // game state stuff
    final static int PLAYSTATE=0,NEWSTATE=1;
    int gamestate=PLAYSTATE;
    // initialize a new game
    public void newgame(boolean cookie){
        debuglog("initializing new game");
        playerdata=new int[squaresize2*squaresize2];
        puz=new puzzle(squaresize,maxremoved,maxcycles);
        //int[] data={2,8,0,0,0,0,4,7,3,5,3,4,8,2,7,1,9,6,0,7,1,0,3,4,0,8,0,3,0,0,5,0,0,0,4,0,0,0,0,3,4,0,0,6,0,4,6,0,7,9,0,3,1,0,0,9,0,2,0,3,6,5,4,0,0,3,0,0,9,8,2,1,0,0,0,0,8,0,9,3,7};
        //puz=new puzzle(data);
        //puz.solve(false,puz.hidden,puz.data);
        gamestate=PLAYSTATE;
        hints=null;
        if(cookie){savecookie();}
    }
    // applet things
    @Override
    public void start(){
        debuglog("starting");
        addKeyListener(this);
        addMouseListener(this);
        add(new renderer(this));
        newgame(false);
        loadcookie();
        new Thread(this).start();
    }
    @Override
    public void run(){
        debuglog("entering main loop");
        repaint();
        while(true){
            if(!isActive()){return;}
        }
    }
    // make sure the selection cursor stays in the grid boundaries (helps prevent nasty array exceptions)
    private void checkselectionbounds(){
        if(selection!=null){
            if(selection[0]<0){selection[0]=0;}
            if(selection[1]<0){selection[1]=0;}
            if(selection[0]>=squaresize2){selection[0]=squaresize2-1;}
            if(selection[1]>=squaresize2){selection[1]=squaresize2-1;}
        }
    }
    // happens when the player hits the 'h' key
    public void togglehints(){
        debuglog("refreshing hints");
        if(hints==null){
            // no list of hints already? make one.
            hints=gethints();
        }else{
            // already a list of hints? check: if putting up the new hints would show the same old hints already up, take the hints down. otherwise, put the new hints up.
            LinkedList<int[]> newhints=gethints();
            if(hintsareequal(hints,newhints)){
                hints=null;
            }else{
                hints=newhints;
            }
        }
    }
    // fill currently listed hints with the correct answers
    public void fillhints(){
        debuglog("filling current hints");
        if(hints!=null){
            for(int[] coord:hints){
                if(getplayerdata(coord[0],coord[1])==0){
                    setplayerdata(coord[0],coord[1],puz.getdata(coord[0],coord[1]));
                }
            }
        }
    }
    // toggle error behavior: when it's on, incorrect selected numbers have a red-tinted backdrop.
    public void toggleerrors(){
        debuglog("toggling error display");
        highlighterrors=!highlighterrors;
    }
    // bring up the new game confirmation
    public void requestnewgame(){
        gamestate=NEWSTATE;
    }
    // return a list of coords that can be filled in now, without any unknown numbers needing to get involved
    public LinkedList<int[]> gethints(){
        boolean[] hidden=puz.hidden.clone();
        int[] data=puz.data.clone();
        for(int i=0;i<hidden.length;i++){
            if(playerdata[i]>0 && puz.hidden[i]){
                hidden[i]=false;
                data[i]=playerdata[i];
            }
        }
        puz.solve(true,hidden,data);
        LinkedList<int[]> ret=(LinkedList<int[]>)puz.hints.clone();
        return ret;
    }
    // because LinkedList.equals() just wasn't cutting it for me
    private boolean hintsareequal(LinkedList<int[]> list1,LinkedList<int[]> list2){
        if(list1==null || list2==null){return (list1==list2);}
        if(list1.equals(list2)){return true;}
        if(list1.size()!=list2.size()){return false;}
        Iterator it1 = list1.iterator();
        Iterator it2 = list2.iterator();
        while(it1.hasNext() && it2.hasNext()){
            int[] coord1=(int[])it1.next();
            int[] coord2=(int[])it2.next();
            if(!(coord1[0]==coord2[0] && coord1[1]==coord2[1])){return false;}
        }
        return true;
    }
    // cookies, yum
    final static String website="http://www.pineapplemachine.com";
    final static String cookiename="JujuSaveData";
    // save the game as a browser cookie
    public void savecookie(){
        debuglog("saving a cookie");
        // honestly I have no idea how any of this works I'm just going off an example on docs.oracle.com
        try{
            CookieManager manager=new CookieManager();
            CookieHandler.setDefault(manager);
            CookieStore store=manager.getCookieStore();
            HttpCookie cookie=new HttpCookie(cookiename,tocookie());
            cookie.setPath("/");
            cookie.setVersion(0);
            cookie.setSecure(false);
            cookie.setMaxAge(60*60*24*365*100);
            cookie.setComment("Juju save data");
            URL url=new URL(website);
            store.add(url.toURI(),cookie);
            debuglog("saved a cookie");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    // load the game from a cookie
    public void loadcookie(){
        debuglog("attempting to load a cookie");
        // still don't know what I'm doing
        try{
            CookieManager manager=new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);
            URL url=new URL(website);
            URLConnection connection=url.openConnection();
            connection.getContent();
            CookieStore store=manager.getCookieStore();
            List<HttpCookie> cookies=store.getCookies();
            for(HttpCookie cookie:cookies){
                if(cookie.getName().equals(cookiename)){
                    fromcookie(cookie.getValue());
                    debuglog("loaded a cookie");
                    break;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    // make a string for saving as a cookie
    public String tocookie(){
        String str="";
        str+=highlighterrors?"1":"0";
        for(int i=0;i<playerdata.length;i++){
            str+=Integer.toString(playerdata[i]);
        }
        return str+puz.tocookie();
    }
    // read puzzle data from a cookie string
    public void fromcookie(String str){
        highlighterrors=(str.charAt(0)=='1');
        if(str.length()>(playerdata.length+1)){
            for(int i=0;i<playerdata.length;i++){
                playerdata[i]=str.charAt(i+1);
            }
        }
        puz.fromcookie(str,playerdata.length+1);
    }    
    // mouse input
    @Override
    public void mouseClicked(MouseEvent e){
        debuglog("mouse clicked event");
        requestFocus();
        int mousex=e.getX();
        int mousey=e.getY();
        if(gamestate==PLAYSTATE){
            selection=getmousecoord(mousex,mousey);
            checkselectionbounds();
            repaint();
        }
    }
    @Override
    public void mouseEntered(MouseEvent e){}
    @Override
    public void mouseExited(MouseEvent e){}
    @Override
    public void mouseReleased(MouseEvent e){}
    @Override
    public void mousePressed(MouseEvent e){}
    // keyboard input
    @Override
    public void keyPressed(KeyEvent e){
        debuglog("key pressed event");
        int code=e.getKeyCode();
        if(gamestate==PLAYSTATE){
            // handle selection cursor movement
            movekeypressed(code);
            // number input
            numkeypressed(code);
            // misc. keyboard controls
            misckeypressed(code);
        }else if(gamestate==NEWSTATE){
            // newgame confirmation keys
            newkeypressed(code);
        }
        repaint();
    }
    @Override
    public void keyReleased(KeyEvent e){}
    @Override
    public void keyTyped(KeyEvent e){}
    private void movekeypressed(int code){
        boolean up=code==KeyEvent.VK_UP || code==KeyEvent.VK_W;
        boolean down=code==KeyEvent.VK_DOWN || code==KeyEvent.VK_S;
        boolean left=code==KeyEvent.VK_LEFT || code==KeyEvent.VK_A;
        boolean right=code==KeyEvent.VK_RIGHT || code==KeyEvent.VK_D;
        int x=(right?1:0)-(left?1:0);
        int y=(down?1:0)-(up?1:0);
        if(x!=0 || y!=0){
            if(selection==null){
                int half=squaresize2/2;
                int[] newselection={half-(x*half),half-(y*half)};
                selection=newselection;
            }else{
                selection[0]+=x;
                selection[1]+=y;
                checkselectionbounds();
            }
        }
    }
    private void numkeypressed(int code){
        if(selection!=null){
            if(code>=KeyEvent.VK_1 && code<=KeyEvent.VK_9){
                inputplayerdata(selection[0],selection[1],code-KeyEvent.VK_1+1);
            }else if(code>=KeyEvent.VK_NUMPAD1 && code<=KeyEvent.VK_NUMPAD9){
                inputplayerdata(selection[0],selection[1],code-KeyEvent.VK_NUMPAD1+1);
            }else if(code==KeyEvent.VK_DELETE || code==KeyEvent.VK_BACK_SPACE || code==KeyEvent.VK_0 || code==KeyEvent.VK_NUMPAD0){
                setplayerdata(selection[0],selection[1],0);
            }
            savecookie();
        }
    }
    private void misckeypressed(int code){
        if(code==KeyEvent.VK_H){
            togglehints();
        }else if(code==KeyEvent.VK_F){
            fillhints();
        }else if(code==KeyEvent.VK_T){
            toggleerrors();
            savecookie();
        }else if(code==KeyEvent.VK_N){
            requestnewgame();
        }
    }
    private void newkeypressed(int code){
        if(code==KeyEvent.VK_Y){
            newgame(true);
        }else if(code==KeyEvent.VK_N){
            gamestate=PLAYSTATE;
        }
    }
    // used for drawing and stuff
    int[] getdimensions(){
        int appletwidth=getWidth(),appletheight=getHeight();
        int xspace=appletwidth-xmargin-xmargin,yspace=appletheight-ymargin-ymargin;
        int space=(xspace<yspace)?xspace:yspace;
        int gridsize=(space-(squaresize+1))/squaresize2;
        space=(gridsize*squaresize2)+(squaresize+1);
        int truexmargin=xmargin+(xspace-space)/2,trueymargin=ymargin+(yspace-space)/2;
        int dim[]={appletwidth,appletheight,space,gridsize,truexmargin,trueymargin};
        return dim;
    }
    // gets the screen space coord from a puzzle coord
    public int[] getcoord(int i,int j,int gridsize){
        int coord[]={i*gridsize+(i/squaresize)+1,j*gridsize+(j/squaresize)+1};
        return coord;
    }
    // get the puzzle coord for a mouse coord (or any coord in screen space, really)
    int[] getmousecoord(int x,int y){
        int appletwidth,appletheight,space,gridsize,truexmargin,trueymargin;
        int dim[]=getdimensions();
        appletwidth=dim[0];appletheight=dim[1];space=dim[2];gridsize=dim[3];truexmargin=dim[4];trueymargin=dim[5];
        x-=truexmargin+1;
        y-=trueymargin+1;
        if(x<0 || y<0 || x>space || y>space){return null;}
        int ssize=(gridsize*squaresize+1);
        int sqx=x/ssize,sqy=y/ssize;
        x=sqx*squaresize+(x-sqx*ssize)/gridsize;
        y=sqy*squaresize+(y-sqy*ssize)/gridsize;
        int[] coord={x,y};
        return coord;
    }
    // getters and setters for numbers that the player has filled in
    private void setplayerdata(int x,int y,int newdata){
        try{playerdata[x+y*squaresize2]=newdata;}
        catch(Exception e){}
    }
    final public int getplayerdata(int x,int y){
        try{return playerdata[x+y*squaresize2];}
        catch(Exception e){return 0;}
    }
    // same as setplayerdata, except it removes any hint coords at the same location.
    public void inputplayerdata(int x,int y,int newdata){
        setplayerdata(x,y,newdata);
        if(hints!=null && !hints.isEmpty()){
            Iterator it=hints.iterator();
            while(it.hasNext()){
                int[] coord=(int[])it.next();
                if(x==coord[0] && y==coord[1]){
                    it.remove();
                    break;
                }
            }
        }
    }
}
