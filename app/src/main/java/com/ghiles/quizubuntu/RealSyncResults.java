package com.ghiles.quizubuntu;

import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

final class RealSyncResults {
    /** JGit can return normally even when a server rejects a branch update. */
    static String requireAccepted(Iterable<PushResult> results) {
        StringBuilder output = new StringBuilder(); boolean rejected=false; int count=0;
        for (PushResult result : results) {
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                count++;
                boolean ok=update.getStatus()==RemoteRefUpdate.Status.OK;
                boolean current=update.getStatus()==RemoteRefUpdate.Status.UP_TO_DATE;
                rejected |= !ok&&!current;
                output.append(update.getRemoteName()).append(" : ")
                    .append(ok?"envoyé":current?"déjà à jour": "REFUSÉ ("+update.getStatus()+")");
                if(update.getMessage()!=null&&!update.getMessage().isEmpty()) output.append(" — ").append(update.getMessage());
                output.append('\n');
            }
        }
        if(count==0)throw new IllegalStateException("Aucune référence confirmée par le serveur : envoi non confirmé.");
        if(rejected)throw new IllegalStateException("Push incomplet ou refusé.\n"+output+"Fais git status, puis récupère les changements distants avec git pull. Aucun push forcé automatique.");
        return output.toString();
    }
    static String changedFiles(org.eclipse.jgit.lib.Repository repository, org.eclipse.jgit.lib.ObjectId before,
                               org.eclipse.jgit.lib.ObjectId after) throws java.io.IOException {
        if (java.util.Objects.equals(before, after)) return "Déjà à jour : aucun nouveau commit récupéré.\n";
        try (org.eclipse.jgit.lib.ObjectReader reader = repository.newObjectReader();
             org.eclipse.jgit.diff.DiffFormatter diff = new org.eclipse.jgit.diff.DiffFormatter(new java.io.ByteArrayOutputStream())) {
            diff.setRepository(repository);
            org.eclipse.jgit.treewalk.AbstractTreeIterator oldTree = new org.eclipse.jgit.treewalk.EmptyTreeIterator();
            org.eclipse.jgit.treewalk.AbstractTreeIterator newTree = new org.eclipse.jgit.treewalk.EmptyTreeIterator();
            if (before != null) {
                org.eclipse.jgit.treewalk.CanonicalTreeParser tree = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                tree.reset(reader, repository.resolve(before.name() + "^{tree}")); oldTree = tree;
            }
            if (after != null) {
                org.eclipse.jgit.treewalk.CanonicalTreeParser tree = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                tree.reset(reader, repository.resolve(after.name() + "^{tree}")); newTree = tree;
            }
            StringBuilder result = new StringBuilder("Fichiers mis à jour :\n");
            for (org.eclipse.jgit.diff.DiffEntry entry : diff.scan(oldTree, newTree))
                result.append(entry.getChangeType()).append(" ").append(entry.getChangeType()==org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE?entry.getOldPath():entry.getNewPath()).append('\n');
            return result.toString();
        }
    }
}
