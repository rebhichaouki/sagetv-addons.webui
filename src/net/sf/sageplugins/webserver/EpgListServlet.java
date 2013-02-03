
package net.sf.sageplugins.webserver;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.sageplugins.sageutils.SageApi;
import net.sf.sageplugins.sageutils.Translate;

/**
 * @author Owner
 *
 * 
 *
 */
public class EpgListServlet extends EpgServlet {

	/**
     * 
     */
    private static final long serialVersionUID = -6981632838485114461L;
    /* (non-Javadoc)
	 * @see net.sf.sageplugins.webserver.SageServlet#doServletGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doServletGet(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {
		
		
		GregorianCalendar start=getStartDate(req);
		Long starttime=new Long(start.getTimeInMillis());
		
		int numhrs=Integer.parseInt(GetOption(req,"list_num_hours","12"));
		GregorianCalendar end=(GregorianCalendar)start.clone();
		end.add(Calendar.HOUR_OF_DAY,numhrs);
		Long endtime=new Long(end.getTimeInMillis());
		
		int numchans=Integer.parseInt(GetOption(req,"list_num_chans","4"));
		Vector<?> channels=getChannels(req,numchans);
		
		htmlHeaders(resp);
		noCacheHeaders(resp);
		PrintWriter out = getGzippedWriter(req,resp);
        // must catch and report all errors within Gzipped Writer
		try {
		    xhtmlHeaders(out);
		    out.println("<head>");
		    jsCssImport(req, out);
		    out.println("<title>TV Guide</title>");
		    out.println("</head>");
		    out.println("<body>");
		    printTitle(out,"TV guide", SageServlet.isTitleBroken(req));
		    out.println("<div id=\"content\">");
		    SimpleDateFormat fmt=new SimpleDateFormat("EEE, MMM d");
		    int cellheight=Integer.parseInt(GetOption(req,"list_cell_height","120"));
		    int cellwidth=Integer.parseInt(GetOption(req,"list_cell_width","120"));
		    boolean showChannelLogos=GetOption(req,"UseChannelLogos","true").equalsIgnoreCase("true");
		    boolean showTimes=GetOption(req,"list_show_time","true").equalsIgnoreCase("true");
		    boolean showDesc=GetOption(req,"list_show_desc","false").equalsIgnoreCase("true");
		    // ouput day/hour selectors
		    printDayHourChannelSelectors(out,req,numhrs);
		    Object RecSchedList=SageApi.Api("GetScheduledRecordings");
		    
		    out.println("<table cellspacing=\"0\" class=\"epglist\"><tr>");
		    out.println("<td>\r\n" +
		    "<table cellspacing=\"0\" class=\"timcol\">");
		    out.println("   <tr><td class=\"datecell\"><div>\r\n"+
		            "      <a href=\""+buildLink(req,-1,0,0)+"\" title=\"Previous Day\"><img class=\"prevday\" alt=\"Arrow Left\" src=\"left.gif\"/></a>\r\n"+
		            "      "+fmt.format(new Date(starttime.longValue()))+
		            "      <a href=\""+buildLink(req,1,0,0)+"\" title=\"Next Day\"><img class=\"nextday\" alt=\"Arrow Right\" src=\"right.gif\"/></a>\r\n"+
		    "   </div></td></tr>");
		    GregorianCalendar hours=(GregorianCalendar)start.clone();
		    DateFormat timefmt=DateFormat.getTimeInstance(DateFormat.SHORT);
		    for(int i=0;i<numhrs;i++) {
		        out.println("   <tr><td class=\"timecell\"><div  style=\"height:"+Long.toString(cellheight-8)+"px\">");
		        if ( i==0){
		            out.println("      <a href=\""+buildLink(req,0,-numhrs,0)+"\" title=\"Earlier\"><img class=\"earlier\" alt=\"Arrow Up\" src=\"up.gif\"/></a>");
		        }
		        if ( i==numhrs-1)
		            out.println("      <a href=\""+buildLink(req,0,numhrs,0)+"\" title=\"Later\"><img class=\"later\" alt=\"Arrow Down\" src=\"down.gif\"/></a>");
		        out.println(timefmt.format(hours.getTime()));
		        out.println("   </div></td></tr>");
		        hours.add(Calendar.HOUR_OF_DAY,1);
		    }
		    out.println("</table></td>");
		    String[] chlist=req.getParameterValues("Channel");
            boolean markHDTV=getMarkHDTV(req);
            boolean markFirstRuns=getMarkFirstRuns(req);
            boolean showEpisodeID=GetOption(req,"ShowEpisodeID","false").equalsIgnoreCase("true");
		    for ( int i=0; i<channels.size(); i++){
		        Object channel=channels.get(i);
		        out.println("<td><table cellspacing=\"0\" class=\"channelcol\"><tr>");
		        out.println("   <td class=\"channelid\"><div style=\"width:"+Long.toString(cellwidth)+"px;\">");
                String chID=SageApi.Api("GetStationID",new Object[]{channel}).toString();
                out.print("      <a href=\"EpgChannel?&amp;startchan=" + chID + "\" title=\"Channel Guide\">");
		        out.println(SageApi.Api("GetChannelNumber",new Object[]{channel}).toString()+
		        " - ");
		        String chname=SageApi.Api("GetChannelName",new Object[]{channel}).toString();	
		        if ( showChannelLogos && null != SageApi.Api("GetChannelLogo",channel)) {
		            out.println("         <img src=\"ChannelLogo?ChannelID="+chID+"&type=Med&index=1&fallback=true\" alt=\""+Translate.encode(chname)+" logo\" title=\""+Translate.encode(chname)+"\"/>");
		        } else {
		            out.println(Translate.encode(chname));
		        }
                out.println("      </a>");
		        if ( i==0 && !(chlist!=null && chlist.length>0 ))
		            out.println("         <a href=\""+buildLink(req,0,0,-channels.size())+"\" title=\"page left\"><img alt=\"Arrow Left\" class=\"pageleft\" src=\"left.gif\"/></a>");
		        if ( i==channels.size()-1 && !(chlist!=null && chlist.length>0 ))
		            out.println("         <a href=\""+buildLink(req,0,0,channels.size())+"\" title=\"page right\"><img alt=\"Arrow Right\"  class=\"pageright\" src=\"right.gif\"/></a>");
                out.println("</div></td></tr>");
		        Object airings=SageApi.Api("GetAiringsOnChannelAtTime",new Object[]{channel,starttime,endtime, Boolean.FALSE});
                Object allConflicts=SageApi.Api("GetAiringsThatWontBeRecorded",new Object[]{Boolean.FALSE});
                Object unresolvedConflicts=SageApi.Api("GetAiringsThatWontBeRecorded",new Object[]{Boolean.TRUE});
		        long skippedDuration=0;
		        for ( int j=0;j<SageApi.Size(airings);j++){
		            Airing airing=new Airing(SageApi.GetElement(airings,j));
		            long cellstart=airing.getStartDate().getTime();
		            if ( cellstart<starttime.longValue())
		                cellstart=starttime.longValue();
		            long cellend=airing.getEndDate().getTime();
		            if ( cellend>endtime.longValue())
		                cellend=endtime.longValue();
		            long thiscellheight=(((skippedDuration+cellend-cellstart)/1000/60)*cellheight/60)-8;

                    String ep=airing.getEpisode();
                    if (  ep != null && ep.trim().length()==0)
                        ep =null;
                    // only look for episode IDs for Airings, or for TV files
                    if ( showEpisodeID && 
                            ( airing.idType == Airing.ID_TYPE_AIRING 
                                    || (airing.idType == Airing.ID_TYPE_MEDIAFILE 
                                            && SageApi.booleanApi("IsTVFile",new Object[]{airing.sageAiring} )
                                    )
                            ) 
                    ){
                        // from malore's menus
                        //If(Size(GetShowExternalID(Airing))>=12,GetShowExternalID(Airing),"00000000000000000000")
                        //If(Substring(DummyEpisodeNum, 8, 12) != "0000",  Substring(DummyEpisodeNum, 8, 12), "") 
                        String epId=(String)SageApi.Api("GetShowExternalID",airing.sageAiring);
                        int epIdx = epId.length() - 4; // show the last 4 chars
                        if ( epId != null
                             && epId.length()>=12 
                             && ! epId.substring(epIdx).matches("^0*$"))
                            if (  ep != null )
                                ep=epId.substring(epIdx)+" - "+ep;
                            else
                                ep=epId.substring(epIdx);
                    }

                    String titletext=airing.getTitle();
                    String alttext=Translate.encode(titletext);
                    if ( titletext.trim().length()==0)
                        titletext="&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                    else 
                        titletext=Translate.encode(titletext);
		            if ( ep != null && ! ep.equals("")){
		                ep=Translate.encode(ep);
		                alttext=titletext+"\r\n"+ep;
		                titletext+="<br/>"+ep;
		            } 
		            if ( thiscellheight<=0){
		                skippedDuration+=cellend-cellstart;
		            }else{
		                skippedDuration=0;
		                String desc="";
		                if ( showDesc){
		                    desc=SageApi.StringApi("GetShowDescription", new Object[]{airing.sageAiring});
		                    if ( desc != null && desc.length()>0){
		                        if ( desc.length()>150)
		                            desc=desc.substring(0,150)+"...";
		                    }
		                }
		                out.println("   <tr><td class=\"epgcell\">");
		                out.println("   <table class=\""+airing.getBgClassName(true)+"\" cellspacing=\"0\"><tr>\r\n"+
		                        "<td class=\""+airing.getBorderClassName()+"\"\r\n"+
		                        "title=\""+alttext+
		                        "\r\n"+
		                        timefmt.format(airing.getStartDate())+ " - "+
		                        timefmt.format(airing.getEndDate())+"\r\n"+
		                        desc+"\""+
		                        //" onclick=\"window.location='DetailedInfo?AiringId="+Integer.toString(airing.id)+"'\"" +
		                ">");
		                if ( airing.getWatched())
		                    out.print("   <div class=\"watched\"");
		                else
		                    out.print("   <div class=\"\"");
		                out.println(" style=\"width:"+Long.toString(cellwidth)+"px; height:"+Long.toString(thiscellheight)+"px\">");
		                // note -- no spaces between <img and titletext to prevent word-wrapping
                        if (markHDTV || markFirstRuns)
                        {
                            out.print("     <img src=\"MarkerHDTVDot.gif\" class=\"hd\" title=\"High Definition\"");
                            if (!airing.getHDTV() || !markHDTV) {
                                out.print(" style=\"visibility: hidden;\"");
                            }
                            out.println("></img>");
                            out.print("     <img src=\"MarkerFirstRunDot.gif\" class=\"firstrun\" title=\"First Run\"");
                            if (!airing.getFirstRun() || !markFirstRuns) {
                                out.print(" style=\"visibility: hidden;\"");
                            }
                            out.println("></img>");
                            out.println("<br/>");
                        }
		                out.print("<a "+
		                        "title=\""+alttext+
		                        "\r\n"+
		                        timefmt.format(airing.getStartDate())+ " - "+
		                        timefmt.format(airing.getEndDate())+"\r\n"+
		                        desc+
		                        "\" href=\"DetailedInfo?"+airing.getIdArg()+"\">");
		                if ( 1==SageApi.Size(SageApi.Api("DataIntersection",new Object[]{RecSchedList,airing.sageAiring}))) {
		                    out.print("<img src=\"recording.gif\" class=\"RecordingIndicator\" alt=\"scheduled to record\"/>");
                        } else {
                            // conflict icon
                            // (Manual?) conflicts at some point got their own airing id so the epg airing does not show up on the conflicts list
                            if (SageApi.booleanApi("IsManualRecord", new Object[]{airing.sageAiring}) &&
                                SageApi.Api("GetMediaFileForAiring", airing.sageAiring) == null) {
                                out.print("<img src=\"conflicticon.gif\" class=\"UnresolvedConflictIndicator\" alt=\"Unresolved Conflict\" title=\"Unresolved Conflict\"/>");
                            } else if ( 1==SageApi.Size(SageApi.Api("DataIntersection",new Object[]{allConflicts,airing.sageAiring}))) {
                                if ( 1==SageApi.Size(SageApi.Api("DataIntersection",new Object[]{unresolvedConflicts,airing.sageAiring})))  {
                                    out.print("<img src=\"conflicticon.gif\" class=\"UnresolvedConflictIndicator\" alt=\"Unresolved Conflict\" title=\"Unresolved Conflict\"/>");
                                } else {
                                    out.print("<img src=\"resolvedconflicticon.gif\" class=\"ResolvedConflictIndicator\" alt=\"Resolved Conflict\" title=\"Resolved Conflict\"/>");
                                }
                            }
		                }
		                out.println(titletext+"</a><br/>\r\n");
		                if ( showTimes){
		                    out.println("<br/>"+
		                            timefmt.format(airing.getStartDate())+ " - "+
		                            timefmt.format(airing.getEndDate()));
		                }
		                if ( showDesc){
		                    out.println("<br/>"+Translate.encode(desc));
		                }
		                out.println("   </div></td></tr></table></td></tr>");
		            }
		        }
		        out.println("</table></td>");
		    }
		    out.println("</tr></table>");
		    out.println("<div class=\"exphideall\">\r\n"+
		    "<a onclick=\"javascript:showOptions()\" href=\"#Options\">[Show Options]</a></div>");
		    out.println("<div id=\"options\" class=\"options\">\r\n" +
		            "<h2><a name=\"Options\">EPG List Options:</a></h2>\r\n" +
		            "<form method='get' action='SetOptions'>\r\n" +
		            "<input type=\"hidden\" name=\"returnto\" value=\""+req.getRequestURI()+"?"+req.getQueryString()+"\"/>\r\n" +
		    "<dl><dt>Cell width (pixels)</dt><dd>");
		    PrintOptionsDropdown(req,out,"list_cell_width",Integer.toString(cellwidth),CELL_SIZE_OPTS);
		    out.println("</dd><dt>Cell height (pixels/hr)</dt><dd>");
		    PrintOptionsDropdown(req,out,"list_cell_height",Integer.toString(cellwidth),CELL_SIZE_OPTS);
		    out.println("</dd><dt>Number of hours to display</dt><dd>");
		    PrintOptionsDropdown(req,out,"list_num_hours",Integer.toString(numhrs),NUM_HRS_OPTS);
		    out.println("</dd><dt>Number of channels to display</dt><dd>");
		    PrintOptionsDropdown(req,out,"list_num_chans",Integer.toString(numchans),NUM_CHANS_OPTS);
		    out.println("</dd><dt>Channel&nbsp;Logos:&nbsp;</dt><dd>");
		    PrintOptionsDropdown(req,out,"UseChannelLogos","true",ENABLE_DISABLE_OPTS);
		    out.println("</dd><dt>Show start/end times</dt><dd>");
		    PrintOptionsDropdown(req,out,"list_show_time","false",ENABLE_DISABLE_OPTS);
		    out.println("</dd><dt>Show description</dt><dd>");
		    PrintOptionsDropdown(req,out,"list_show_desc","false",ENABLE_DISABLE_OPTS);
            out.println("</dd><dt>Show&nbsp;HDTV&nbsp;Marker</dt><dd>");
            PrintOptionsDropdown(req,out,"epg_mark_hdtv","##AsSageTV##",EPG_MARKER_OPTS);
            out.println("</dd><dt>Show&nbsp;First&nbsp;Run&nbsp;Marker</dt><dd>");
            PrintOptionsDropdown(req,out,"epg_mark_first_runs","##AsSageTV##",EPG_MARKER_OPTS);
            out.println("</dd><dt>Include&nbsp;EpisodeID:&nbsp;</dt><dd>");
            PrintOptionsDropdown(req,out,"ShowEpisodeID","false",ENABLE_DISABLE_OPTS);
		    out.println("</dd></dl><noscript><input type=\"submit\" value=\"SetOptions\"/></noscript></form>\r\n" +
		            "<form method='get' action='"+req.getRequestURI()+"'>\r\n" +
		            "<dl><dt>Custom Channel List (Use CTRL+click to select multiple channels):\r\n" +
		    "</dt><dd>");
		    out.println("<select name=\"Channels\" multiple=\"multiple\" size=\"10\">");
		    String[] Channels=req.getParameterValues("Channels");
		    List<String> Channels_l=null;
		    if ( Channels!=null )
		        Channels_l=Arrays.asList(Channels);
		    else
		        Channels_l=new Vector<String>();
		    Object channellist=SageApi.Api("GetAllChannels");
		    channellist=SageApi.Api("Sort",new Object[]{channellist,Boolean.FALSE,"GetChannelNumber"});
		    for ( int i=0;i<SageApi.Size(channellist);i++) {
		        Object chan=SageApi.GetElement(channellist,i);
		        if ( SageApi.booleanApi("IsChannelViewable",new Object[] {chan}) ){
		            String channame=SageApi.StringApi("GetChannelName",new Object[] {chan});
		            String channumber=SageApi.StringApi("GetChannelNumber",new Object[] {chan});
		            String chanId=Integer.toString(SageApi.IntApi("GetStationID",new Object[] {chan}));
		            
		            out.print("   <option value=\""+chanId+"\"");
		            if ( Channels != null && Channels.length>0 && Channels_l.contains(chanId))
		                out.print(" selected=\"selected\"");
		            out.println(">"+Translate.encode(channumber+" -- "+ channame)+"</option>");
		        }
		    }
		    out.println("</select></dd></dl>\r\n" +
		            "<input type=\"submit\" value=\"Show Custom Channel List\"/>\r\n"+
		    "</form>");
		    out.println("</div>\r\n" +// options
		    "<script type=\"text/javascript\">hideOptions();</script>"); 
		    
		    out.println();
            printFooter(req,out);
            out.println("</div>");//content
		    printMenu(req,out);
		    out.println("</body></html>");
		    out.close();
        } catch (Throwable e) {
            if (!resp.isCommitted()){
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/html");
            }
            out.println();
            out.println();
            out.println("<body><pre>");
            out.println("Exception while processing servlet:\r\n"+e.toString());
            e.printStackTrace(out);
            out.println("</pre>");
            out.close();
            log("Exception while processing servlet",e);
        }
	}
	static final String[][] CELL_SIZE_OPTS=new String[][]{
			{"30","very small (30px)"},
			{"60","small (60px)"},
			{"120","medium (120px)"},
			{"240","large (240px)"},
			{"360","very large (360px)"},
			{"480","huge! (480px)"},
	};
}

	
