package com.cruson.review;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NotificationFieldsTest {

	@Test
	public void testGetAuthorName() {
		String author = "author";
		String revision = "revision";

		assertEquals(author,
				NotificationFields.getAuthorName(author + "@" + revision));
	}
}
