package com.hust.globalict.main.components;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import com.hust.globalict.main.utils.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class LocalizationUtils {
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;

	public String getLocalizedMessage(String messageKey, Object... params) {// spread operator
		HttpServletRequest request = WebUtils.getCurrentRequest();
		Locale locale = localeResolver.resolveLocale(request);
		return messageSource.getMessage(messageKey, params, locale);
	}
}
