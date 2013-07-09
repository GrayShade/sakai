<%@ page import="javax.portlet.RenderRequest" %>
<%@ page import="javax.portlet.RenderResponse" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.PortletMode" %>
<%@ page import="javax.portlet.PortletSession" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.util.List" %>
<%@ page import="org.sakaiproject.util.ResourceLoader" %>
<%@ page import="org.sakaiproject.util.Validator" %>

<%@ page session="false" %>
<%!

private static ResourceLoader rb = new ResourceLoader("basiclti");

// Allow the user to set unless final.launch etc. is true
public boolean allow(Properties props, String s) {
  if ( props == null || s == null ) return false;
  String prop = props.getProperty("final."+s); 
  if ( prop == null ) return true;
  if ( "true".equals(prop) ) return false;
  return true;
}
%>
<%
RenderResponse rResp = (RenderResponse)request.getAttribute("javax.portlet.response");
RenderRequest rReq = (RenderRequest)request.getAttribute("javax.portlet.request");
PortletSession pSession = rReq.getPortletSession(true);

PortletURL viewURL = rResp.createActionURL();
viewURL.setParameter("sakai.action","main");

PortletURL clearURL = rResp.createActionURL();
clearURL.setParameter("sakai.action","edit.clear");

PortletURL resetURL = rResp.createActionURL();
resetURL.setParameter("sakai.action","edit.reset");

PortletURL launchURL = rResp.createActionURL();
launchURL.setParameter("sakai.action","edit.save");

PortletURL actionURL = rResp.createActionURL();

String errorMsg = (String) rReq.getAttribute("error.message");

Properties ov = (Properties) rReq.getAttribute("imsti.oldvalues");

Properties sp = (Properties) rReq.getAttribute("imsti.properties");

List<String> assignments = (List<String>) rReq.getAttribute("assignments");

Boolean allowOutcomes = (Boolean) rReq.getAttribute("allowOutcomes");

Boolean allowSettings = (Boolean) rReq.getAttribute("allowSettings");

Boolean allowRoster = (Boolean) rReq.getAttribute("allowRoster");

Boolean allowLori = (Boolean) rReq.getAttribute("allowLori");

Boolean allowContentLink = (Boolean) rReq.getAttribute("allowContentLink");

%>
<portlet:defineObjects/>
<div class="portletBody">
<ul class="navIntraTool actionToolBar">
	<li class="firstToolBarItem">
		<span>
			<a href="<%=viewURL.toString()%>"><%=rb.getString("edit.exit")%></a>
		</span>
	</li>	
ri
	<li>
		<span>
			<a href="<%=resetURL.toString()%>"><%=rb.getString("edit.clear.prefs")%></a>
		</span>
	</li>
</ul>	
<% 
    if ( errorMsg != null ) { %>
		<div class="alertMessage"><%= errorMsg %></div>
	<% }

    if ( allow(sp,"launch") || allow(sp,"key") || allow(sp,"secret") || 
        allow(sp,"xml") ||
        allow(sp,"pagetitle") || allow(sp,"tooltitle") ||
        allow(sp,"resource") || allow(sp,"preferwidget") || 
        allow(sp,"height") || allow(sp,"width") || 
        allow(sp,"frameheight") || allow(sp,"custom") || 
        allow(sp, "releasename") || allow(sp,"releaseemail")  ||
        allow(sp, "allowroster") || allow(sp,"allowsettings") ||
        allow(sp, "allowlori")
) { %>
<form method="post" action="<%=launchURL.toString()%>">
<% if ( allow(sp,"launch") || allow(sp,"xml") || allow(sp,"key") || allow(sp,"secret") ) { %>

<h3><%=rb.getString("required.information") %></h3>
<% if ( allow(sp,"launch") && allow(sp,"xml") ) { %>
<script type="text/javascript">
function switchui()
{
  var x=document.getElementById("UISwitcher");
  if ( x.selectedIndex == 0 ) {
    document.getElementById("xml.paste").style.display = "none";
    document.getElementById("url.input").style.display = "block";
  } else {
    document.getElementById("url.input").style.display = "none";
    document.getElementById("xml.paste").style.display = "block";
  }
}
</script>
<select id="UISwitcher" name="imsti.type" onchange="switchui(); return false;">
  <option value="URL"><%=rb.getString("select.url") %></option>
<% if ( ov.getProperty("imsti.xml",null) != null ) { %>
  <option selected="selected" value="XML"><%=rb.getString("select.xml") %></option>
<% } else { %>
  <option value="XML"><%=rb.getString("select.xml")%></option>
<% } %>
</select>
<% } %>
<% if ( allow(sp,"xml") ) { %>
<div id="xml.paste">
<textarea rows="20" cols="60"  name="imsti.xml" >
<%=ov.getProperty("imsti.xml","")%>
</textarea>
<%=rb.getString("tool.xml.detail") %>
</div>
<% } %>
<% if ( allow(sp,"launch") ) { %>
<p id="url.input" class="shorttext" style="clear:none;">
<span class="reqStar">*</span><label for="imsti.launch"><%=rb.getString("tool.url") %></label><br/>
<input type="text" size="80" name="imsti.launch" id="imsti.launch" value="<%=ov.getProperty("imsti.launch","")%>">
<%=rb.getString("tool.url.detail") %>
</p>
<% } %>
<% if ( allow(sp,"key") ) { %>
<p  class="shorttext" style="clear:none;">
<span class="reqStar">*</span><label for="imsti.key"><%=rb.getString("tool.key") %></label>
<input type="text" name="imsti.key" id="imsti.key" value="<%=ov.getProperty("imsti.key","")%>"> 
<%=rb.getString("tool.key.detail") %>
</p>
<% } %>
<% if ( allow(sp,"secret") ) { %>
<p  class="shorttext" style="clear:none;">
<span class="reqStar">*</span>	<label for="imsti.secret"><%=rb.getString("tool.secret") %></label>
<input type="password" name="imsti.secret" id="imsti.secret" value="<%=ov.getProperty("imsti.secret","")%>"> 
<%=rb.getString("tool.secret.detail") %>
</p>
<% } %>

<% } %>
<script type="text/javascript">
if ( document.getElementById("UISwitcher") ) switchui();
</script>

<% if ( allow(sp,"pagetitle") || allow(sp,"tooltitle") ) { %>

<h3><%=rb.getString("display.information") %></h3>
<% if ( allow(sp,"pagetitle") ) { %>
<p  class="shorttext" style="clear:none;">
<label for="imsti.pagetitle"><%=rb.getString("page.title") %></label>
<input type="text" name="imsti.pagetitle" id="imsti.pagetitle" value="<%=ov.getProperty("imsti.pagetitle","")%>"> 
<span class="textPanelFooter"><%=rb.getString("page.title.detail") %></span>
</p>
<% } %>
<% if ( allow(sp,"tooltitle") ) { %>
<p  class="shorttext" style="clear:none;">
<label for="imsti.tooltitle"><%=rb.getString("tool.title") %></label>
<input type="text" name="imsti.tooltitle" id="imsti.tooltitle" size="40" value="<%=ov.getProperty("imsti.tooltitle","")%>"> 
<span class="textPanelFooter"><%=rb.getString("tool.title.detail") %></span>
</p>
<% } %>

<% } %>

<% if ( allowOutcomes && allow(sp,"allowoutcomes") ) { %>
<h3><%=rb.getString("gradable.information") %></h3>
<p  class="shorttext" style="clear:none;">
<label for="imsti.newassignment"><%=rb.getString("gradable.newassignment") %></label>
<input type="text" size="10" name="imsti.newassignment" id="imsti.newassignment" value="<%=ov.getProperty("imsti.newassignment","")%>"> 
<span class="textPanelFooter"><%=rb.getString("gradable.newassignment.detail") %></span>
</p>

<% } %>

<% if ( allowOutcomes && allow(sp,"allowoutcomes") && assignments != null ) { %>
<p  class="shorttext" style="clear:none;">
<%=rb.getString("gradable.title") %>
<select name="imsti.assignment">
  <option value=""><%=rb.getString("gradable.nograde") %></option>
<% for ( String assn : assignments ) { 
     if ( Validator.escapeHtml(assn).equals(ov.getProperty("imsti.assignment")) ) { %>
       <option selected="selected" value="<%=Validator.escapeHtml(assn) %>"><%=Validator.escapeHtml(assn) %></option>
<%   } else { %>
       <option value="<%=Validator.escapeHtml(assn) %>"><%=Validator.escapeHtml(assn) %></option>
<%   }
   } %>
</select>
<span class="textPanelFooter"><%=rb.getString("gradable.detail") %></span>
</p>

<% } %>

<% if ( allow(sp,"frameheight") || allow(sp, "debug") || allow(sp, "newpage") || allow(sp, "maximize") ) { %>
<h3><%=rb.getString("launch.information") %></h3>
<% if ( allow(sp,"newpage") ) { %>
<p class="checkbox indnt1">
<input type="checkbox" size="10" name="imsti.newpage" id="imsti.newpage" 
<% if ( ov.getProperty("imsti.newpage",null) != null ) { %>
  checked="yes" />
<% } else { %>
   />
<% } %>
<label for="imsti.newpage"><%=rb.getString("new.page") %></label><%=rb.getString("new.page.detail") %>
</p>
<% } %>
<% if ( allow(sp,"maximize") ) { %>
<p class="checkbox  indnt1">
<input type="checkbox" size="10" name="imsti.maximize" id="imsti.maximize" 
<% if ( ov.getProperty("imsti.maximize",null) != null ) { %>
  checked="yes" />
<% } else { %>
   />
<% } %>
<label for="imsti.maximize"><%=rb.getString("maximize.page") %></label>
<span class="textPanelFooter"><%=rb.getString("maximize.page.detail") %></span>
</p>
<% } %>
<% if ( allow(sp,"frameheight") ) { %>
<p  class="shorttext" style="clear:none;">
<label for="imsti.frameheight"><%=rb.getString("iframe.height") %></label>
<input type="text" size="10" name="imsti.frameheight" id="imsti.frameheight" value="<%=ov.getProperty("imsti.frameheight","")%>"> 
<span class="textPanelFooter"><%=rb.getString("iframe.height.detail") %></span>
</p>
<% } %>
<% if ( allow(sp,"debug") ) { %>
<p class="checkbox indnt1">
<input type="checkbox" size="10" name="imsti.debug" id="imsti.debug" 
<% if ( ov.getProperty("imsti.debug",null) != null ) { %>
  checked="yes" />
<% } else { %>
   />
<% } %>
<label for="imsti.debug"><%=rb.getString("debug.launch") %></label>
<span class="textPanelFooter"><%=rb.getString("debug.launch.detail") %></span>
</p>
<% } %>

<% } %>

<% if ( allow(sp,"releasename") || allow(sp, "releaseemail") || 
        ( allow(sp, "allowroster") && allowRoster )  || 
        ( allow(sp, "allowlori") && allowLori ) 
) { %>
<h3><%=rb.getString("launch.privacy") %></h3>
<% if ( allow(sp,"releasename") ) { %>
	<p class="checkbox indnt1">
		<input type="checkbox" size="10" name="imsti.releasename" id="imsti.releasename" 
		<% if ( ov.getProperty("imsti.releasename",null) != null ) { %>
  		checked="yes" />
			<% } else { %>
   		/>
		<% } %>
		<label for="imsti.releasename"><%=rb.getString("privacy.releasename") %></label>
	</p>
<% } %>
<% if ( allow(sp,"releaseemail") ) { %>
	<p class="checkbox indnt1">
		<input type="checkbox" size="10" name="imsti.releaseemail" id="imsti.releaseemail" 
		<% if ( ov.getProperty("imsti.releaseemail",null) != null ) { %>
  		checked="yes" />
		<% } else { %>
   		/>
		<% } %>
	<label for="imsti.releaseemail"><%=rb.getString("privacy.releaseemail") %></label>	
	<span class="textPanelFooter"><%=rb.getString("launch.privacy.detail") %></span>
</p>
<% } %>
<% if ( allow(sp,"allowroster") && allowRoster ) { %>
<p>

<input type="checkbox" size="10" name="imsti.allowroster" id="imsti.allowroster" 
<% if ( ov.getProperty("imsti.allowroster",null) != null ) { %>
  checked="yes" />
<% } else { %>
   />
<% } %>
<label for="imsti.allowroster"><%=rb.getString("privacy.allowroster") %></label>
<span class="textPanelFooter"><%=rb.getString("allowroster.detail") %></span>
</p>
<% } %>
<% if ( allow(sp,"allowlori") && allowLori ) { %>
<p>

<input type="checkbox" size="10" name="imsti.allowlori" id="imsti.allowlori" 
<% if ( ov.getProperty("imsti.allowlori",null) != null ) { %>
  checked="yes" />
<% } else { %>
   />
<% } %>
<label for="imsti.allowlori"><%=rb.getString("privacy.allowlori") %></label>
<span class="textPanelFooter"><%=rb.getString("allowlori.detail") %></span>
</p>
<% } %>

<% } %>

<% if ( allow(sp,"contentlink") && allowContentLink ) { %>
<h3><%=rb.getString("contentlink.legend") %></h3>
<p class="shorttext" style="clear:none">
<label for="imsti.contentlink"><%=rb.getString("contentlink.label") %></label><br/>
<input type="text" name="imsti.contentlink" size="80" id="imsti.contentlink" value="<%=ov.getProperty("imsti.contentlink","")%>"> 
<span class="textPanelFooter"><%=rb.getString("contentlink.detail") %></span>
</p>
<% } %>


<% if ( allow(sp,"allowsettings") && allowSettings ) { %>
<h3><%=rb.getString("allowsettings.information") %></h3>
<p class="checkbox">

<input type="checkbox" size="10" name="imsti.allowsettings" id="imsti.allowsettings" 
<% if ( ov.getProperty("imsti.allowsettings",null) != null ) { %>
  checked="yes" />
<% } else { %>
   />

<% } %>
<label for="imsti.allowsettings"><%=rb.getString("privacy.allowsettings") %></label>
<span class="textPanelFooter"><%=rb.getString("allowsettings.detail") %></span>
</p>

<% } %>

<% if ( allow(sp,"custom") ) { %>
<h3><%=rb.getString("launch.custom") %></h3>
<p class="longtext">
<label for="imsti.custom" class="textPanelFooter" style="float:none;display:block;width:50%"><%=rb.getString("launch.custom.detail") %></label>
<textarea rows="10" cols="60"  name="imsti.custom" id="imsti.custom" >
<%=ov.getProperty("imsti.custom","")%>
</textarea>

</p>
<% } %>
<p>
<input type="submit" value="<%=rb.getString("update.options")%>">
<input type="submit" value="<%=rb.getString("edit.exit")%>" 
    onclick="window.location='<%=viewURL.toString()%>'; return false;"/>
</p>
</form>
<% } else { %>
<p class="instruction">Configuration has been pre-set and cannot be edited for this placement.</p>
<% } %>
</div>
