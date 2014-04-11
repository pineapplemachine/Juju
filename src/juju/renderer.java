/*
 * Juju is Sudoku written in Java! This program and its source code are
 * released as public domain. (But that doesn't mean you should claim credit
 * that isn't yours, or sell it when it could otherwise be had for free, 
 * because that would be a shitty thing of you to do.)
 * Juju was written by Sophie Kirschner. Visit the author's website at
 * http://pineapplemachine.com
 */

package juju;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

// class responsible for doing the rendering (doing it in a JPanel conveniently avoids flickering)
public class renderer extends JPanel {
    // visual settings
    final static Color backgroundcolor=new Color(150,220,235);
    final static Color solidcolor=new Color(8,8,20);
    final static Color usercolor=new Color(65,70,200);
    final static Color errorcolor=new Color(175,160,200);
    final static Color selectioncolor=new Color(150,170,200);
    final static Color hintcolor=new Color(215,210,170);
    final static Color textcolor=new Color(140,150,190);
    final static Font numberfont=new Font("SansSerif",Font.BOLD,12);
    final static Font textfont=new Font("SansSerif",Font.BOLD,6);
    // game object
    main game;
    // constructor
    public renderer(main maingame){
            super.setBackground(backgroundcolor);
            game=maingame;
    }
    // do the actual redering, yay!
    @Override
    public void paintComponent(Graphics g){
        // do some funky stuff to figure out just where to draw stuff
        int appletwidth,appletheight,space,gridsize,truexmargin,trueymargin;
        int dim[]=game.getdimensions();
        appletwidth=dim[0];appletheight=dim[1];space=dim[2];gridsize=dim[3];truexmargin=dim[4];trueymargin=dim[5];
        // draw background
        g.setColor(backgroundcolor);
        g.fillRect(0,0,appletwidth,appletheight);
        // draw text stuff
        painttext(g,appletwidth,appletheight);
        // draw hints
        painthints(g,gridsize,truexmargin,trueymargin);
        // draw the selection thing
        paintselection(g,gridsize,truexmargin,trueymargin);
        // draw the gridlines
        paintgrid(g,space,gridsize,truexmargin,trueymargin);
        // draw the numbers
        paintnumbers(g,gridsize,truexmargin,trueymargin);
        // draw newgame confirmation
        if(game.gamestate==main.NEWSTATE){
            paintnewgame(g,appletwidth,appletheight,gridsize);
        } 
    }
    public void painttext(Graphics g,int appletwidth,int appletheight){
        g.setColor(textcolor);
        g.drawString("Juju",3,13);
        g.drawString("www.pineapplemachine.com",appletwidth-162,appletheight-3);
    }
    public void painthints(Graphics g,int gridsize,int truexmargin,int trueymargin){
        if(game.hints!=null && !game.hints.isEmpty()){
            g.setColor(hintcolor);
            for(int[] hintcoord:game.hints){
                int[] coord=game.getcoord(hintcoord[0],hintcoord[1],gridsize);
                int x=coord[0]+truexmargin;
                int y=coord[1]+trueymargin;
                g.fillRect(x,y,gridsize,gridsize);
            }
        }
    }
    public void paintselection(Graphics g,int gridsize,int truexmargin,int trueymargin){
        if(game.selection!=null && game.selection[0]>=0 && game.selection[1]>=0){
            int[] coord=game.getcoord(game.selection[0],game.selection[1],gridsize);
            int x=coord[0]+truexmargin;
            int y=coord[1]+trueymargin;
            Color drawcolor=selectioncolor;
            int selplayerdata=game.getplayerdata(game.selection[0],game.selection[1]);
            if(game.highlighterrors && selplayerdata>0 && game.puz.gethidden(game.selection[0],game.selection[1]) && game.puz.getdata(game.selection[0],game.selection[1])!=selplayerdata){drawcolor=errorcolor;}
            g.setColor(drawcolor);
            g.fillRect(x,y,gridsize,gridsize);
        }
    }
    public void paintgrid(Graphics g,int space,int gridsize,int truexmargin,int trueymargin){
        g.setColor(solidcolor);
        int l=0;
        for(int i=0;i<=main.squaresize2;i++){
            g.drawLine(truexmargin+l,trueymargin,truexmargin+l,trueymargin+space);
            g.drawLine(truexmargin,trueymargin+l,truexmargin+space,trueymargin+l);
            if((i%main.squaresize)==0){
                l++;
                g.drawLine(truexmargin+l,trueymargin,truexmargin+l,trueymargin+space);
                g.drawLine(truexmargin,trueymargin+l,truexmargin+space,trueymargin+l);
            }
            l+=gridsize;
        }
    }
    public void paintnumbers(Graphics g,int gridsize,int truexmargin,int trueymargin){
        g.setFont(numberfont);
        FontMetrics fm=g.getFontMetrics();
        for(int i=0;i<main.squaresize2;i++){
            for(int j=0;j<main.squaresize2;j++){
                int drawnum=0;
                Color drawcolor=usercolor;
                int playernum=game.getplayerdata(i,j);
                if(!game.puz.gethidden(i,j)){
                    drawnum=game.puz.getdata(i,j);
                    drawcolor=solidcolor;
                }else if(playernum>0){
                    drawnum=playernum;
                } 
                if(drawnum>0){
                    int[] coord=game.getcoord(i,j,gridsize);
                    String str=Integer.toString(drawnum);
                    Rectangle2D rect=fm.getStringBounds(str,g);
                    int x=coord[0]+truexmargin+(gridsize-(int)rect.getWidth())/2;
                    int y=coord[1]+trueymargin+(gridsize-(int)rect.getHeight())/2;
                    g.setColor(drawcolor);
                    g.drawString(str,x+1,y+(int)rect.getHeight()-2);
                }
            }
        }
    }
    public void paintnewgame(Graphics g,int appletwidth,int appletheight,int gridsize){
        int rectheight=gridsize-1;
        g.setColor(solidcolor);
        g.drawRect(0,(appletheight-rectheight)/2,appletwidth,rectheight);
        g.setColor(backgroundcolor);
        g.fillRect(0,(appletheight-rectheight)/2+1,appletwidth,rectheight-1);
        g.setColor(solidcolor);
        g.setFont(numberfont);
        FontMetrics fm=g.getFontMetrics();
        String str="New game? Y/N";
        Rectangle2D rect=fm.getStringBounds(str,g);
        g.drawString(str,(appletwidth-(int)rect.getWidth())/2+1,(appletheight-(int)rect.getHeight())/2+(int)rect.getHeight()-2);
    }
}
