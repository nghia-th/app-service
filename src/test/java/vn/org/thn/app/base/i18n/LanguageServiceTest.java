package vn.org.thn.app.base.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.org.thn.app.base.i18n.api.LanguageRequest;
import vn.org.thn.app.base.i18n.domain.Language;
import vn.org.thn.app.base.i18n.domain.Translate;
import vn.org.thn.app.base.i18n.repository.TranslateRepository;
import vn.org.thn.app.base.i18n.service.LanguageService;
import vn.org.thn.app.base.persistence.lambda.SFunction;
import vn.org.thn.app.base.persistence.query.DeleteBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    @Mock
    private Language language;

    @Mock
    private TranslateRepository translateRepository;

    @InjectMocks
    private LanguageService languageService;

    @BeforeEach
    void setUp() {
        lenient().when(language.getValues()).thenReturn(Map.of());
    }

    @Test
    @DisplayName("Should update language and save entity through repository")
    void updateLanguage_validRequest_success() {
        LanguageRequest request = new LanguageRequest();
        request.setLangKey("btn.save");
        request.setMapValues(Map.of("vi", "Lưu", "en", "Save"));

        when(translateRepository.save(any(Translate.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> languageService.updateLanguage(request));

        verify(language, times(1)).updateLanguage(request);
        verify(translateRepository, times(2)).save(any(Translate.class));
    }

    @Test
    @DisplayName("Should delete language key from memory and repository")
    void deleteLanguage_validKey_success() {
        @SuppressWarnings("unchecked")
        DeleteBuilder<Translate> deleteBuilder = Mockito.mock(DeleteBuilder.class);
        when(deleteBuilder.eq(any(SFunction.class), any())).thenReturn(deleteBuilder);
        when(deleteBuilder.execute()).thenReturn(1);
        when(translateRepository.delete()).thenReturn(deleteBuilder);

        assertDoesNotThrow(() -> languageService.deleteLanguage("btn.save"));

        verify(language, times(1)).delete("btn.save");
        verify(translateRepository, times(1)).delete();
    }
}
