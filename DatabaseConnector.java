import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import vtk.*;

public class DatabaseConnector 
{	
	
	   public static final char LF = '\n';

	   public static final char CR = '\r';
	
	   //GET ORGANISM LIST FROM DATABASE TABLES
		public String[] getOrganismList()
		{
			List<String> organisms = new ArrayList<String>();
			Connection con;
			DatabaseMetaData m;
			ResultSet tables;
			try{
				Class.forName("com.mysql.jdbc.Driver");
				//Information should be the same; "EDP" should be changed to password of user you are using on your system
				con = DriverManager.getConnection("jdbc:mysql://localhost:3306/organism","root","EDP");
				m = con.getMetaData();
				tables = m.getTables(null, null, null,null);
				while(tables.next())
				{
					organisms.add(tables.getString(3));
				}
				
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
			}
			
			
		
			
				
			String[] organismList= new String[organisms.size()];
			organisms.toArray(organismList);
			return organismList;
			
		
		}
		
		//GET ORGANISM LIST FROM URL INFORMATION
		public String[] getOrganismListURLDriven(HashMap<String,Integer> NameToTaxID) throws Exception
		{
	
			URL url = new URL("http://webservice.thebiogrid.org/organisms/?accesskey=457329496765df827aec5829b6b6d26a");
			URLConnection urlConnect=url.openConnection();
			InputStreamReader in;
			String s;
			ArrayList<String> tempValues = new ArrayList<String>();
			ArrayList<String> organisms = new ArrayList<String>();
			TabDelimitedReader tabRead = new TabDelimitedReader();
			
			if(urlConnect!=null)
				urlConnect.setReadTimeout(60*1000);
			if(urlConnect!=null)
			{
				in = new InputStreamReader(urlConnect.getInputStream());
				BufferedReader buffRead = new BufferedReader(in);
				while((s=buffRead.readLine())!=null)
				{
					tempValues = tabRead.readLine(s);
					NameToTaxID.put(tempValues.get(1),Integer.parseInt(tempValues.get(0)));
					organisms.add(tempValues.get(1));
					tempValues.clear();
				}
			}
				
			String[] organismList= new String[organisms.size()];
			organisms.toArray(organismList);
			return organismList;
		
		}
		
		public void getGeneInfo(vtkMutableUndirectedGraph graph, int gene,String organism, JEditorPane editorPane)
		{
			vtkStringArray geneNames = (vtkStringArray) graph.GetVertexData().GetAbstractArray("labels");
			
		//	styleSheet.addRule("pre {font : 10px monaco; color : black; background-color : #fafafa; }");
			
			
			try
			{
				Class.forName("com.mysql.jdbc.Driver");
				
				Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/organism","root","EDP");
				PreparedStatement statement = con.prepareStatement("SELECT * from "+organism+" WHERE BINARY Official_Symbol_Interactor_A ='" + geneNames.GetValue(gene) +"' OR BINARY Official_Symbol_Interactor_B = '" + geneNames.GetValue(gene) +"'");
				ResultSet rs = statement.executeQuery();
				String s = "";
				s+="<pre>";
				s+="<b>";
				s += String.format("%55s",geneNames.GetValue(gene));
				s+="</b>";
				s+="</pre>";
				s += "<br>";
				s+="<pre>";
				s+="<u>";
				s += String.format("%-20s%-30s%-40s%-40s%-20s", "Gene", "Experimental System", "Experimental System Type", "Author","Link");
				s+="</u>";
				s+="</pre>";
				s += "<br>";
				while(rs.next())
				{
					if(!rs.getString("Official_Symbol_Interactor_A").equals(geneNames.GetValue(gene)))
					{
						s+="<pre>";
						s += String.format("%-20s%-30s%-40s%-40s", rs.getString("Official_Symbol_Interactor_A"), rs.getString(12), rs.getString(13),rs.getString("Author"));
						s += "<a href='" + "http://www.ebi.ac.uk/europepmc/webservices/rest/search/resulttype=core&query=ext_id:"+rs.getString(15) + "'>Link</a>";
						s+="</pre>";
					}
					else
					{
						s+="<pre>";
						s += String.format("%-20s%-30s%-40s%-40s", rs.getString("Official_Symbol_Interactor_B"), rs.getString(12), rs.getString(13), rs.getString("Author"));
						s += "<a href='" + "http://www.ebi.ac.uk/europepmc/webservices/rest/search/resulttype=core&query=ext_id:"+rs.getString(15) + "'>Link</a>";
						s+="</pre>";
					}
					s += "<br>";
				}
				
				
					try {
						HTMLEditorKit htmlKit = new HTMLEditorKit();
						editorPane.setEditorKit(htmlKit);
						StyleSheet styleSheet = htmlKit.getStyleSheet();
						styleSheet.addRule("pre { display: inline;}");
						styleSheet.addRule("a {display:inline; }");
					      Document doc = htmlKit.createDefaultDocument();
					      editorPane.setDocument(doc);
					      editorPane.setText(s);
					     
					} catch(Exception exc) {
					      exc.getMessage();
					}
				
			}
			catch(Exception e)
			{
				e.getMessage();
			}
			
			
		}
		
		
		//Used for taking information from extended graph
		public void getGeneInfo(ExtendedGraph graph, int gene, JEditorPane editorPane)
		{	
			//Relevant arrays needed to reference
			vtkStringArray geneNames = (vtkStringArray) graph.getGraph().GetVertexData().GetAbstractArray("labels");
			vtkIntArray edgeWeights = (vtkIntArray) graph.getGraph().GetEdgeData().GetAbstractArray("weights");
			
			//Temp arrays to print edge data
			ArrayList<String> author= new ArrayList<String>();
			ArrayList<String> system= new ArrayList<String>();
			ArrayList<String> systemType= new ArrayList<String>();
			ArrayList<String> pubMedID= new ArrayList<String>();
			
			//Interacting gene
			String interactingGene;
			
		//	styleSheet.addRule("pre {font : 10px monaco; color : black; background-color : #fafafa; }");
			vtkEdgeListIterator iterator = new vtkEdgeListIterator();
			
			
			//Heading
			
			String s = "";
			s+="<pre>";
			s+="<b>";
			s += String.format("%55s",geneNames.GetValue(gene));
			s+="</b>";
			s+="</pre>";
			s += "<br>";
			s+="<pre>";
			s+="<u>";
			s += String.format("%-20s%-30s%-40s%-40s%-20s%-20s", "Gene", "Experimental System", "Experimental System Type", "Author","Abstract/Title","Link");
			s+="</u>";
			s+="</pre>";
			s += "<br>";
			
			//Heading done
			
			
			graph.getGraph().GetEdges(iterator);
			int edgeCount=0;
			vtkGraphEdge edge;
			
			while(iterator.HasNext())
			{
				edge=iterator.NextGraphEdge();
				if(geneNames.GetValue(edge.GetSource()).equals(geneNames.GetValue(gene)))
				{
					author=graph.getAuthor().get(edgeCount);
					system=graph.getSystem().get(edgeCount);
					systemType=graph.getSystemType().get(edgeCount);
					pubMedID=graph.getPubMedID().get(edgeCount);
					System.out.println(system.get(0));
					for(int i=0;i<edgeWeights.GetValue(edgeCount);i++)
					{
						s+="<pre>";
						s += String.format("%-20s%-30s%-40s%-40s", geneNames.GetValue(edge.GetTarget()), system.get(i), systemType.get(i),author.get(i));
						s += "<a href='" + "http://www.ebi.ac.uk/europepmc/webservices/rest/search/resulttype=core&query=ext_id:"+pubMedID.get(i) + "'>Abstract/Title</a>";
						s+="      ";
						s += "<a href='" + "http://www.ncbi.nlm.nih.gov/pubmed/"+pubMedID.get(i) + "'>Link</a>";
						s+="</pre>";
					}
				}
				else if(geneNames.GetValue(edge.GetTarget()).equals(geneNames.GetValue(gene)))
				{
					author=graph.getAuthor().get(edgeCount);
					system=graph.getSystem().get(edgeCount);
					systemType=graph.getSystemType().get(edgeCount);
					pubMedID=graph.getPubMedID().get(edgeCount);
					for(int i=0;i<edgeWeights.GetValue(edgeCount);i++)
					{
						s+="<pre>";
						s += String.format("%-20s%-30s%-40s%-40s", geneNames.GetValue(edge.GetSource()), system.get(i), systemType.get(i),author.get(i));
						s += "<a href='" + "http://www.ebi.ac.uk/europepmc/webservices/rest/search/resulttype=core&query=ext_id:"+pubMedID.get(i) + "'>Abstract/Title</a>";
						s+="      ";
						s += "<a href='" + "http://www.ncbi.nlm.nih.gov/pubmed/"+pubMedID.get(i) + "'>Link</a>";
						s+="</pre>";
					}
				}
			}
			
			try {
				HTMLEditorKit htmlKit = new HTMLEditorKit();
				editorPane.setEditorKit(htmlKit);
				StyleSheet styleSheet = htmlKit.getStyleSheet();
				styleSheet.addRule("pre { display: inline;}");
				styleSheet.addRule("a {display:inline; }");
			      Document doc = htmlKit.createDefaultDocument();
			      editorPane.setDocument(doc);
			      editorPane.setText(s);
			     
			} catch(Exception exc) {
			      exc.getMessage();
			}			
		}	
}
