package com.cruson.review;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

@RunWith(MockitoJUnitRunner.class)
public class CrucibleNotificationChannelTest {
    @Mock
    private CrucibleApi api;

    @Mock
    private PropertiesDao propertiesDao;

    @Mock
    private HttpDownload download;

    private CrucibleNotificationChannel channel;

    private Notification notification;

    private Settings globalSettings;

    @Before
    public void setUp() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(CruSonPlugin.CRUSON_HOST_URL, "http://cnx:8060");
        properties.put(CruSonPlugin.CRUSON_HOST_LOGIN, "test");
        properties.put(CruSonPlugin.CRUSON_HOST_PASSWORD, "1234");
        properties.put(CruSonPlugin.CRUSON_PROJECT, "test");
        properties.put(CruSonPlugin.CRUSON_REPOSITORY, "YuraTest");
        globalSettings = new Settings();
        globalSettings.setProperties(properties);

        channel = new CrucibleNotificationChannel(globalSettings,
                propertiesDao, download);
        channel = spy(channel);

        notification = new Notification(NotificationFields.NOTIFICATION_TYPE)
                .setFieldValue(NotificationFields.PROJECT_KEY, "gitest")
                .setFieldValue(NotificationFields.COMPONENT_PATH,
                        "PROJECT_KEY:src/main/java/sonar/git/gitest/TestGit.java")
                .setFieldValue(NotificationFields.SEVERITY, "SEVERITY")
                .setFieldValue(NotificationFields.SCM_AUTHOR,
                        "yura <yura@nixsolutions.com>")
                .setFieldValue(NotificationFields.LINE, "9")
                .setFieldValue(NotificationFields.SCM_DATE, "SCM_DATE")
                .setFieldValue(NotificationFields.SCM_REVISION,
                        "6f67f322e7fb490cc8ee116b21fec64af97d792e")
                .setFieldValue(NotificationFields.SCM_REVISION_LAST,
                        "6f67f322e7fb490cc8ee116b21fec64af97d7921")
                .setFieldValue(NotificationFields.RULE_KEY, "squid:S106")
                .setFieldValue(NotificationFields.MESSAGE, "MESSAGE");
    }

    @Test
    public void testBuildProjectSettings() throws Exception {
        globalSettings.clear();
        PropertyDto dto = new PropertyDto();
        dto.setKey("key");
        dto.setValue("value");

        when(propertiesDao.selectProjectProperties("gitest")).thenReturn(
                Arrays.asList(new PropertyDto[] { dto }));

        ProjectSettings projectSettings = channel
                .buildProjectSettings("gitest");

        Assert.assertEquals("value", projectSettings.getString("key"));
    }

    @Test
    public void testBuildCrucibleApi() throws Exception {
        CrucibleApiImpl apiImpl = (CrucibleApiImpl) channel
                .buildCrucibleApi(globalSettings);
        Assert.assertEquals("test", apiImpl.getLogin());
        Assert.assertEquals("1234", apiImpl.getPassword());
        Assert.assertEquals("http://cnx:8060", apiImpl.getUrl());
    }

    @Test
    public void testDeliver() throws Exception {
        ProjectSettings settings = new ProjectSettings(globalSettings,
                new HashMap<String, String>());
        doReturn(settings).when(channel).buildProjectSettings("gitest");
        doReturn(api).when(channel).buildCrucibleApi(settings);
        doThrow(new IOException()).when(channel).createReview(settings, api,
                notification);

        channel.deliver(notification, "");

        verify(channel).createReview(settings, api, notification);
    }

    @Test
    public void testCreateReview() throws Exception {
        String reviewId = "reviewId";
        String itemId = "itemId";
        String foundAuthor = "foundAuthor";

        doReturn(foundAuthor).when(channel).searchUser(globalSettings, api,
                notification);
        when(
                api.createReview(eq("test"), eq("MESSAGE"),
                        Mockito.anyString(), eq("test"))).thenReturn(reviewId);
        doNothing().when(api).addReviewer(reviewId, foundAuthor);

        when(
                api.addReviewItem(
                        "YuraTest",
                        reviewId,
                        "PROJECT_KEY:src/main/java/sonar/git/gitest/TestGit.java",
                        "6f67f322e7fb490cc8ee116b21fec64af97d7921"))
                .thenReturn(itemId);

        doNothing().when(api).addReviewComment(eq(reviewId), eq(itemId),
                anyString(), eq("9"));

        doNothing().when(api).startReview(reviewId);

        channel.createReview(globalSettings, api, notification);

        verify(channel).searchUser(globalSettings, api, notification);

        verify(api).createReview(eq("test"), eq("MESSAGE"),
                Mockito.anyString(), eq("test"));
        verify(api).addReviewer(reviewId, foundAuthor);
        verify(api).addReviewItem("YuraTest", reviewId,
                "PROJECT_KEY:src/main/java/sonar/git/gitest/TestGit.java",
                "6f67f322e7fb490cc8ee116b21fec64af97d7921");
        verify(api).addReviewComment(eq(reviewId), eq(itemId), anyString(),
                eq("9"));
        verify(api).startReview(reviewId);
        verifyNoMoreInteractions(api);
    }

    @Test
    public void testSearchUserByCommiter() throws Exception {
        String author = "author";

        when(api.getUserByCommiter("YuraTest", "yura <yura@nixsolutions.com>"))
                .thenReturn(author);

        assertEquals(author,
                channel.searchUser(globalSettings, api, notification));

        verify(api).getUserByCommiter("YuraTest",
                "yura <yura@nixsolutions.com>");
        verifyNoMoreInteractions(api);
    }

    @Test
    public void testSearchUserBySameName() throws Exception {
        when(api.getUserByCommiter("YuraTest", "yura <yura@nixsolutions.com>"))
                .thenReturn(null);
        when(api.isUserExist("yura")).thenReturn(true);

        assertEquals("yura",
                channel.searchUser(globalSettings, api, notification));

        verify(api).getUserByCommiter("YuraTest",
                "yura <yura@nixsolutions.com>");
        verify(api).isUserExist("yura");
        verifyNoMoreInteractions(api);
    }

    @Test
    @Ignore
    public void testMakeReviewData() {
        Notification notification = new Notification(
                NotificationFields.NOTIFICATION_TYPE)
                .setFieldValue(NotificationFields.PROJECT_KEY, "gitest")
                .setFieldValue(NotificationFields.COMPONENT_PATH,
                        "module1/src/main/java/sonar/git/gitest/module1/Test11.java")
                .setFieldValue(NotificationFields.SEVERITY, "SEVERITY")
                .setFieldValue(NotificationFields.SCM_AUTHOR,
                        "yura <yura@nixsolutions.com>")
                .setFieldValue(NotificationFields.LINE, "9")
                .setFieldValue(NotificationFields.SCM_DATE, "SCM_DATE")
                .setFieldValue(NotificationFields.SCM_REVISION,
                        "6abc4938eb7deaf833353b3e1ff8603312809e13")
                .setFieldValue(NotificationFields.SCM_REVISION_LAST,
                        "6abc4938eb7deaf833353b3e1ff8603312809e13")
                .setFieldValue(NotificationFields.RULE_KEY, "squid:S106")
                .setFieldValue(NotificationFields.MESSAGE, "MESSAGE");

        channel = new CrucibleNotificationChannel(globalSettings, null,
                new HttpDownloadImpl());
        channel = spy(channel);

        ProjectSettings settings = new ProjectSettings(globalSettings,
                new HashMap<String, String>());
        doReturn(settings).when(channel).buildProjectSettings("gitest");

        channel.deliver(notification, null);
    }
}
