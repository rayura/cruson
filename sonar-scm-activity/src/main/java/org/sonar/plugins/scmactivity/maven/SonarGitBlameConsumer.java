/*
 * SonarQube SCM Activity Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.scmactivity.maven;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.util.AbstractConsumer;

import com.google.common.annotations.VisibleForTesting;

/**
 * Plain copy of package
 * org.apache.maven.scm.provider.git.gitexe.command.blame.GitBlameConsumer
 * Patched to allow user email retrieval when parsing Git blame results.
 *
 * @Todo: hack - to be submitted as an update in maven-scm-api for a future
 *        release
 *
 *        <p/>
 *        For more information, see: <a
 *        href="http://jira.sonarsource.com/browse/DEVACT-103">DEVACT-103</a>
 *
 * @since 1.5.1
 */
public class SonarGitBlameConsumer extends AbstractConsumer {

    private static final String GIT_COMMITTER_PREFIX = "committer";
    private static final String GIT_COMMITTER_TIME = GIT_COMMITTER_PREFIX
            + "-time ";
    private static final String GIT_AUTHOR = "author ";
    private static final String GIT_AUTHOR_EMAIL = "author-mail ";
    private static final String GIT_COMMITTER_EMAIL = GIT_COMMITTER_PREFIX
            + "-mail ";
    private static final String OPENING_EMAIL_FIELD = "<";
    private static final String CLOSING_EMAIL_FIELD = ">";

    private List<BlameLine> lines = new ArrayList<BlameLine>();

    /**
     * Since the porcelain format only contains the commit information the first
     * time a specific sha-1 commit appears, we need to store this information
     * somwehere.
     * <p/>
     * key: the sha-1 of the commit value: the {@link BlameLine} containing the
     * full committer/author info
     */
    private Map<String, BlameLine> commitInfo = new HashMap<String, BlameLine>();

    private boolean expectRevisionLine = true;

    private String revision = null;
    private String author = null;
    private String authorEmail = null;
    private String committerEmail = null;
    private Date time = null;

    public SonarGitBlameConsumer(ScmLogger logger) {
        super(logger);
    }

    public void consumeLine(String line) {
        if (line == null) {
            return;
        }

        if (expectRevisionLine) {
            // this is the revision line
            consumeRevisionLine(line);
        } else {

            if (extractCommitInfoFromLine(line)) {
                return;
            }

            if (line.startsWith("\t")) {
                // this is the content line.
                // we actually don't need the content, but this is the right
                // time to add the blame line
                consumeContentLine();
            }
        }
    }

    @VisibleForTesting
    protected boolean extractCommitInfoFromLine(String line) {
        if (line.startsWith(GIT_AUTHOR)) {
            author = line.substring(GIT_AUTHOR.length());
            return true;
        }

        if (line.startsWith(GIT_AUTHOR_EMAIL)) {
            authorEmail = extractEmail(line);
            return true;
        }

        if (line.startsWith(GIT_COMMITTER_EMAIL)) {
            committerEmail = extractEmail(line);
            return true;
        }

        if (line.startsWith(GIT_COMMITTER_TIME)) {
            String timeStr = line.substring(GIT_COMMITTER_TIME.length());
            time = new Date(Long.parseLong(timeStr) * 1000L);
            return true;
        }
        return false;
    }

    @VisibleForTesting
    protected String getAuthor() {
        return authorEmail;
    }

    @VisibleForTesting
    protected String getCommitter() {
        return committerEmail;
    }

    @VisibleForTesting
    protected Date getTime() {
        return time;
    }

    private String extractEmail(String line) {

        int emailStartIndex = line.indexOf(OPENING_EMAIL_FIELD);
        int emailEndIndex = line.indexOf(CLOSING_EMAIL_FIELD);

        if (emailStartIndex == -1 || emailEndIndex == -1
                || emailEndIndex <= emailStartIndex) {
            return null;
        }
        return line.substring(emailStartIndex + 1, emailEndIndex);
    }

    private void consumeContentLine() {
        String authorStr = authorEmail;
        if (StringUtils.isNotEmpty(authorEmail)
                && StringUtils.containsNone(authorEmail, "<>")) {
            authorStr = author + " <" + authorEmail + ">";
        }
        BlameLine blameLine = new BlameLine(time, revision, authorStr,
                committerEmail);
        getLines().add(blameLine);

        // keep commitinfo for this sha-1
        commitInfo.put(revision, blameLine);

        if (getLogger().isDebugEnabled()) {
            DateFormat df = SimpleDateFormat.getDateTimeInstance();
            getLogger().debug(authorEmail + " " + df.format(time));
        }

        expectRevisionLine = true;
    }

    private void consumeRevisionLine(String line) {
        String[] parts = line.split("\\s", 4);

        if (parts.length >= 1) {
            revision = parts[0];

            BlameLine oldLine = commitInfo.get(revision);

            if (oldLine != null) {
                // restore the commit info
                authorEmail = oldLine.getAuthor();
                committerEmail = oldLine.getCommitter();
                time = oldLine.getDate();
            }

            expectRevisionLine = false;
        }
    }

    public List<BlameLine> getLines() {
        return lines;
    }
}
