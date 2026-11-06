package scratch;

import java.util.List;
import server.dao.ShareDAO;
import server.model.SharedFile;

public class CheckShare {
    public static void main(String[] args) throws Exception {
        ShareDAO shareDAO = new ShareDAO();
        List<SharedFile> list = shareDAO.getSharedWithUser(39);
        System.out.println("shareDAO.getSharedWithUser(39) returned " + list.size() + " items:");
        for (SharedFile sf : list) {
            System.out.println(" - ID " + sf.getShareId() + ": " + sf.getFilename() + " (" + sf.getFileSize() + " bytes) from " + sf.getOwnerUsername() + " [Perm: " + sf.getPermission() + "] Token: " + sf.getShareToken());
        }
    }
}
