package cz.iocb.chemweb.server.servlets.sources;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.vecmath.Point2d;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.cip.CIPTool;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.interfaces.ITetrahedralChirality;
import org.openscience.cdk.io.DefaultChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.postgresql.PostgresDatabase;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.shared.services.DatabaseException;



@SuppressWarnings("serial")
public class CompoundImageServlet extends HttpServlet
{
    //private static String compoundQuery = "select molfile from compounds where id=?";
    //private static PGPoolingDataSource connectionPool = null;
    private final SparqlDatabaseConfiguration dbConfig;
    private final PostgresDatabase db;
    //private final Decoder base64Decoder;


    /*
    static synchronized void initConnectionPool() throws FileNotFoundException, IOException, NumberFormatException,
            SQLException, PropertyVetoException
    {
        if(connectionPool != null)
            return;

        Properties properties = new Properties();
        properties.load(new FileInputStream("orchem.properties"));

        connectionPool = new PGPoolingDataSource();
        connectionPool.setDataSourceName("PubChem Data Source");
        connectionPool.setServerName(properties.getProperty("host"));
        connectionPool.setPortNumber(Integer.parseInt(properties.getProperty("port")));
        connectionPool.setDatabaseName(properties.getProperty("database"));
        connectionPool.setUser(properties.getProperty("user"));
        connectionPool.setPassword(properties.getProperty("password"));
        connectionPool.setSocketTimeout(Integer.parseInt(properties.getProperty("socketTimeout")));
        connectionPool.setTcpKeepAlive(properties.getProperty("tcpKeepAlive").equals("true"));
        connectionPool.setCompatible(properties.getProperty("assumeMinServerVersion"));
        connectionPool.setMaxConnections(Integer.parseInt(properties.getProperty("maxConnections")));
    }
    */


    //private final List<IGenerator<IAtomContainer>> generators;


    public CompoundImageServlet() throws FileNotFoundException, IOException, DatabaseException, SQLException
    {
        //generators = new ArrayList<IGenerator<IAtomContainer>>();
        //generators.add(new BasicSceneGenerator());
        //generators.add(new StandardGenerator(new Font("Verdana", Font.PLAIN, 18)));

        dbConfig = PubChemConfiguration.get();
        db = new PostgresDatabase(dbConfig.getConnectionPool());
        //base64Decoder = Base64.getDecoder();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        processRequest(req, res);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        processRequest(req, res);
    }


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        int id;
        int size;

        try
        {
            id = Integer.parseInt(req.getParameter("id").substring(3));
        }
        catch (NullPointerException | NumberFormatException e)
        {
            throw new ServletException("an invalid value of the 'id' argument");
        }

        try
        {
            size = Integer.parseInt(req.getParameter("w"));
        }
        catch (NullPointerException | NumberFormatException e)
        {
            throw new ServletException("an invalid value of the 'w' argument");
        }

        //if(size != 80 && size != 200)
        if(size < 40 || size > 800)
            throw new ServletException("an invalid value of the 'w' argument");


        try
        {
            IAtomContainer molecule = getMolecule(id);

            if(molecule == null)
                throw new ServletException("an invalid value of the 'id' argument");

            res.setContentType("image/png");

            try (ServletOutputStream out = res.getOutputStream())
            {
                generateImage(out, molecule, size, size);
            }
        }
        catch (CDKException | DatabaseException e)
        {
            e.printStackTrace();
            throw new ServletException("cannot generate image output");
        }
    }


    @Override
    public String getServletInfo()
    {
        return "Compound Image Servlet";
    }


    public static void generateImage(OutputStream baos, IAtomContainer molecule, int hsize, int vsize)
            throws IOException, CDKException
    {
        generateImage(baos, molecule, hsize, vsize, new Color(0, 0, 0, 0));
    }


    public static void generateImage(OutputStream baos, IAtomContainer molecule, int hsize, int vsize, Color background)
            throws IOException, CDKException
    {
        boolean missing = false;

        for(IAtom a : molecule.atoms())
            if(a.getPoint2d() == null)
                missing = true;

        if(missing)
        {
            //molecule = generatePoint2d(molecule);

            for(IAtom a : molecule.atoms())
                a.setPoint2d(new Point2d(a.getPoint3d().x, a.getPoint3d().y));
        }



        /***********************************************************/


        List<IGenerator<IAtomContainer>> generators;
        generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new StandardGenerator(new Font("Verdana", Font.PLAIN, 18)));

        AtomContainerRenderer renderer = new AtomContainerRenderer(generators, new AWTFontManager());
        renderer.getRenderer2DModel().set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());
        renderer.getRenderer2DModel().set(StandardGenerator.AnnotationColor.class, new Color(0x455FFF));
        renderer.getRenderer2DModel().set(BasicSceneGenerator.Scale.class, 0.5);

        renderer.getRenderer2DModel().set(StandardGenerator.Visibility.class,
                SymbolVisibility.iupacRecommendationsWithoutTerminalCarbon());

        renderer.getRenderer2DModel().set(StandardGenerator.Visibility.class,
                SymbolVisibility.iupacRecommendationsWithoutTerminalCarbon());


        Rectangle2D bounds = new Rectangle2D.Double(0, 0, hsize, vsize);
        BufferedImage bufferedImage = new BufferedImage(hsize, vsize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setBackground(background);
        graphics.setColor(background);
        graphics.fillRect(0, 0, hsize, hsize);

        renderer.paint(molecule, new AWTDrawVisitor(graphics), bounds, true);
        bufferedImage.flush();

        ImageIO.write(bufferedImage, "png", baos);
    }


    private IAtomContainer getMolecule(int id)
            throws DatabaseException, UnsupportedEncodingException, CDKException, IOException
    {
        //Result result = db
        //        .query("sparql SELECT ?SDF WHERE { GRAPH <http://rdf.ncbi.nlm.nih.gov/pubchem/compound/extra> { compound:CID"
        //                + id + "_Molfile sio:has-value ?SDF } }");

        Result result = db.query("select sdf as \"SDF#str\" from compound_sdfiles where compound = " + id);

        if(result.getCount() == 1)
        {
            /*
            RdfNode node = result.get("SDF");

            byte[] compresedMolfile = base64Decoder.decode(node.getValue());

            ByteArrayInputStream in = new ByteArrayInputStream(compresedMolfile);
            BZip2CompressorInputStream bzip = new BZip2CompressorInputStream(in);
            String molfile = IOUtils.toString(bzip, "ASCII");
            bzip.close();

            IAtomContainer atomContainer = crateAtomContainer(molfile);
            return atomContainer;
            */

            RdfNode node = result.get("SDF");
            IAtomContainer atomContainer = crateAtomContainer(node.getValue());
            return atomContainer;
        }

        return null;
    }


    public static IAtomContainer crateAtomContainer(String molfile) throws CDKException, IOException
    {
        boolean isV3000 = molfile.contains("M  V30 BEGIN CTAB");

        try (DefaultChemObjectReader mdlReader = isV3000 ? new MDLV3000Reader() : new MDLV2000Reader())
        {
            IAtomContainer molecule = new AtomContainer();
            mdlReader.setReader(new StringReader(molfile));
            molecule = mdlReader.read(molecule);

            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);

            ElectronDonation model = ElectronDonation.cdk();
            CycleFinder cycles = Cycles.cdkAromaticSet();
            Aromaticity aromaticity = new Aromaticity(model, cycles);
            aromaticity.apply(molecule);

            CIPTool.label(molecule);
            molecule = removeNonChiralHydrogens(molecule);

            for(IAtom atom : molecule.atoms())
                atom.setProperty(StandardGenerator.ANNOTATION_LABEL, getCipLabel(atom));
            for(IBond bond : molecule.bonds())
                bond.setProperty(StandardGenerator.ANNOTATION_LABEL, getCipLabel(bond));

            return molecule;
        }
    }


    static private String getCipLabel(IChemObject chemObj)
    {
        String label = chemObj.getProperty(CDKConstants.CIP_DESCRIPTOR);

        if(label == null)
            return null;

        return StandardGenerator.ITALIC_DISPLAY_PREFIX + label;
    }


    /**
     * Produces an AtomContainer without explicit non stereo-relevant Hs but with H count from one with Hs. The new
     * molecule is a deep copy.
     *
     * @param org The AtomContainer from which to remove the hydrogens
     * @return The molecule without non stereo-relevant Hs.
     * @cdk.keyword hydrogens, removal
     */
    public static IAtomContainer removeNonChiralHydrogens(IAtomContainer org)
    {

        Map<IAtom, IAtom> map = new HashMap<IAtom, IAtom>(); // maps original atoms to clones.
        List<IAtom> remove = new ArrayList<IAtom>(); // lists removed Hs.

        // Clone atoms except those to be removed.
        IAtomContainer cpy = org.getBuilder().newInstance(IAtomContainer.class);
        int count = org.getAtomCount();

        for(int i = 0; i < count; i++)
        {

            // Clone/remove this atom?
            IAtom atom = org.getAtom(i);

            boolean addToRemove = suppressibleHydrogen(org, atom);

            /*
            boolean addToRemove = false;

            if(suppressibleHydrogen(org, atom))
            {
                // test whether connected to a single hetero atom only, otherwise keep
                if(org.getConnectedAtomsList(atom).size() == 1)
                {
                    IAtom neighbour = org.getConnectedAtomsList(atom).get(0);
                    // keep if the neighbouring hetero atom has stereo information, otherwise continue checking
                    Integer stereoParity = neighbour.getStereoParity();
                    if(stereoParity == null || stereoParity == 0)
                    {
                        addToRemove = true;

                        // keep if any of the bonds of the hetero atom have stereo information
                        for(IBond bond : org.getConnectedBondsList(neighbour))
                        {
                            IBond.Stereo bondStereo = bond.getStereo();

                            if(bondStereo != null && bondStereo != IBond.Stereo.NONE)
                            {
                                addToRemove = false;
                                System.err.println(bondStereo.name());
                            }

                            IAtom neighboursNeighbour = bond.getConnectedAtom(neighbour);
                            // remove in any case if the hetero atom is connected to more than one hydrogen
                            if(neighboursNeighbour.getSymbol().equals("H") && neighboursNeighbour != atom)
                            {
                                addToRemove = true;
                                break;
                            }
                        }
                    }
                }
            }
            */

            if(addToRemove)
                remove.add(atom);
            else
                addClone(atom, cpy, map);
        }

        // rescue any false positives, i.e., hydrogens that are stereo-relevant
        // the use of IStereoElement is not fully integrated yet to describe stereo information
        for(IStereoElement stereoElement : org.stereoElements())
        {
            if(stereoElement instanceof ITetrahedralChirality)
            {
                ITetrahedralChirality tetChirality = (ITetrahedralChirality) stereoElement;
                for(IAtom atom : tetChirality.getLigands())
                {
                    if(atom.getSymbol().equals("H") && remove.contains(atom))
                    {
                        remove.remove(atom);
                        addClone(atom, cpy, map);
                    }
                }
            }
            /**/
            else if(stereoElement instanceof IDoubleBondStereochemistry)
            {
                IDoubleBondStereochemistry dbs = (IDoubleBondStereochemistry) stereoElement;
                IBond stereoBond = dbs.getStereoBond();
                for(IAtom neighbor : org.getConnectedAtomsList(stereoBond.getAtom(0)))
                {
                    if(remove.remove(neighbor))
                        addClone(neighbor, cpy, map);
                }
                for(IAtom neighbor : org.getConnectedAtomsList(stereoBond.getAtom(1)))
                {
                    if(remove.remove(neighbor))
                        addClone(neighbor, cpy, map);
                }
            }
            /**/
        }

        // Clone bonds except those involving removed atoms.
        count = org.getBondCount();
        for(int i = 0; i < count; i++)
        {
            // Check bond.
            final IBond bond = org.getBond(i);
            boolean removedBond = false;
            final int length = bond.getAtomCount();
            for(int k = 0; k < length; k++)
            {
                if(remove.contains(bond.getAtom(k)))
                {
                    removedBond = true;
                    break;
                }
            }

            // Clone/remove this bond?
            if(!removedBond)
            {
                IBond clone = null;
                try
                {
                    clone = org.getBond(i).clone();
                }
                catch (CloneNotSupportedException e)
                {
                    e.printStackTrace();
                }
                assert clone != null;
                clone.setAtoms(new IAtom[] { map.get(bond.getAtom(0)), map.get(bond.getAtom(1)) });
                cpy.addBond(clone);
            }
        }

        // Recompute hydrogen counts of neighbours of removed Hydrogens.
        for(IAtom aRemove : remove)
        {
            // Process neighbours.
            for(IAtom iAtom : org.getConnectedAtomsList(aRemove))
            {
                final IAtom neighb = map.get(iAtom);
                if(neighb == null)
                    continue; // since for the case of H2, neight H has a heavy atom neighbor
                neighb.setImplicitHydrogenCount(
                        (neighb.getImplicitHydrogenCount() == null ? 0 : neighb.getImplicitHydrogenCount()) + 1);
            }
        }
        for(IAtom atom : cpy.atoms())
        {
            if(atom.getImplicitHydrogenCount() == null)
                atom.setImplicitHydrogenCount(0);
        }
        cpy.addProperties(org.getProperties());
        cpy.setFlags(org.getFlags());

        return cpy;
    }


    private static void addClone(IAtom atom, IAtomContainer mol, Map<IAtom, IAtom> map)
    {

        IAtom clonedAtom = null;
        try
        {
            clonedAtom = atom.clone();
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        mol.addAtom(clonedAtom);
        map.put(atom, clonedAtom);
    }


    /**
     * Is the {@code atom} a suppressible hydrogen and can be represented as implicit. A hydrogen is suppressible if it
     * is not an ion, not the major isotope (i.e. it is a deuterium or tritium atom) and is not molecular hydrogen.
     *
     * @param container the structure
     * @param atom an atom in the structure
     * @return the atom is a hydrogen and it can be suppressed (implicit)
     */
    private static boolean suppressibleHydrogen(final IAtomContainer container, final IAtom atom)
    {
        // is the atom a hydrogen
        if(!"H".equals(atom.getSymbol()))
            return false;
        // is the hydrogen an ion?
        if(atom.getFormalCharge() != null && atom.getFormalCharge() != 0)
            return false;
        // is the hydrogen deuterium / tritium?
        if(atom.getMassNumber() != null && atom.getMassNumber() != 1)
            return false;
        // molecule hydrogen with implicit H?
        if(atom.getImplicitHydrogenCount() != null && atom.getImplicitHydrogenCount() != 0)
            return false;
        // molecule hydrogen
        List<IAtom> neighbors = container.getConnectedAtomsList(atom);
        if(neighbors.size() == 1 && neighbors.get(0).getSymbol().equals("H"))
            return false;
        // what about bridging hydrogens?
        // hydrogens with atom-atom mapping?
        return true;
    }



    /**
     * FIXME: use better method!
     *
     * @param molecule
     * @return
     * @throws CDKException
     */
    @SuppressWarnings("unused")
    static private IAtomContainer generatePoint2d(IAtomContainer molecule) throws CDKException
    {
        IAtomContainerSet fragmentSet = ConnectivityChecker.partitionIntoMolecules(molecule);

        List<IAtomContainer> fragments = new ArrayList<IAtomContainer>();

        for(IAtomContainer m : fragmentSet.atomContainers())
            fragments.add(m);

        Collections.sort(fragments, new Comparator<IAtomContainer>()
        {
            @Override
            public int compare(IAtomContainer arg0, IAtomContainer arg1)
            {
                return Integer.compare(arg1.getAtomCount(), arg0.getAtomCount());
            }
        });



        final double delta = 2;
        double gminX = Double.MAX_VALUE;
        double gmaxX = Double.MIN_VALUE;
        double gminY = Double.MAX_VALUE;
        double gmaxY = Double.MIN_VALUE;
        double gwidth = 0;
        double gheight = 0;
        boolean first = true;

        IAtomContainer gmol = new AtomContainer();

        for(IAtomContainer m : fragments)
        {
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            sdg.setMolecule(m);
            sdg.generateCoordinates();
            m = sdg.getMolecule();

            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;

            for(IAtom a : m.atoms())
            {
                minX = Math.min(minX, a.getPoint2d().x);
                maxX = Math.max(maxX, a.getPoint2d().x);
                minY = Math.min(minY, a.getPoint2d().y);
                maxY = Math.max(maxY, a.getPoint2d().y);
            }

            double width = maxX - minX;
            double height = maxY - minY;

            if(first)
            {
                gminX = minX;
                gmaxX = maxX;
                gminY = minY;
                gmaxY = maxY;
                gwidth = width;
                gheight = height;
                first = false;
            }
            else
            {
                double offsetX = 0;
                double offsetY = 0;

                if(gwidth + width < gheight + height)
                {
                    offsetX = gmaxX - minX + delta;
                    offsetY = gminY - minY + (gheight - height) / 2;
                }
                else
                {
                    offsetX = gminX - minX + (gwidth - width) / 2;
                    offsetY = gminY - maxY - delta;
                }

                for(IAtom a : m.atoms())
                {
                    Point2d p = new Point2d();
                    p.x = a.getPoint2d().x + offsetX;
                    p.y = a.getPoint2d().y + offsetY;
                    a.setPoint2d(p);

                    gminX = Math.min(gminX, p.x);
                    gmaxX = Math.max(gmaxX, p.x);
                    gminY = Math.min(gminY, p.y);
                    gmaxY = Math.max(gmaxY, p.y);
                }

                gwidth = gmaxX - gminX;
                gheight = gmaxY - gminY;
            }

            gmol.add(m);
        }

        return gmol;
    }
}
