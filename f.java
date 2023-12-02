import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.PDOutlineItem;

public class PdfTocCreator {

    public static void main(String[] args) {
        String origFileName = "";
        String tocFileName = "";
        int offset = 0;

        try {
            for (int i = 0; i < args.length; i += 2) {
                String option = args[i];
                String value = args[i + 1];

                switch (option) {
                    case "-f":
                    case "--file":
                        origFileName = value;
                        break;
                    case "-o":
                    case "--offset":
                        offset = Integer.parseInt(value);
                        break;
                    case "-t":
                    case "--toc":
                        tocFileName = value;
                        break;
                    default:
                        System.err.println("Invalid option: " + option);
                        printUsage();
                        System.exit(1);
                }
            }

            String newFileName = origFileName.replaceAll("\\.pdf$", "_toc.pdf");
            PDDocument pdfDocument = PDDocument.load(new File(origFileName));
            PDDocumentOutline documentOutline = pdfDocument.getDocumentCatalog().getDocumentOutline();
            if (documentOutline == null) {
                documentOutline = new PDDocumentOutline();
                pdfDocument.getDocumentCatalog().setDocumentOutline(documentOutline);
            }

            PDOutlineItem[] parentsBookmark = new PDOutlineItem[10];
            for (int i = 0; i < parentsBookmark.length; i++) {
                parentsBookmark[i] = null;
            }

            Pattern tabPattern = Pattern.compile("^(\t*)");
            Pattern semicolonPattern = Pattern.compile(";");
            Matcher matcher;

            File tocFile = new File(tocFileName);
            java.util.Scanner scanner = new java.util.Scanner(tocFile);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                matcher = tabPattern.matcher(line);
                matcher.find();
                int tabCnt = matcher.group(1).length();
                if (tabCnt >= 10) {
                    System.out.println("Too many levels");
                    System.exit(2);
                }
                String title = line.split(";")[0].replaceAll("^\\t*", "");
                int pageNo = Integer.parseInt(line.split(";")[1].trim()) + offset;

                PDOutlineItem parent = tabCnt > 0 ? parentsBookmark[tabCnt - 1] : null;
                parentsBookmark[tabCnt] = new PDOutlineItem();
                parentsBookmark[tabCnt].setTitle(title);
                parentsBookmark[tabCnt].setDestination(pdfDocument.getPage(pageNo - 1));

                if (parent != null) {
                    parent.addNext(parentsBookmark[tabCnt]);
                } else {
                    documentOutline.addFirst(parentsBookmark[tabCnt]);
                }
            }

            pdfDocument.save(newFileName);
            pdfDocument.close();
            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java PdfTocCreator -f <pdf_file_name> -o <pageno_offset> -t <toc_file_name>");
    }
}
