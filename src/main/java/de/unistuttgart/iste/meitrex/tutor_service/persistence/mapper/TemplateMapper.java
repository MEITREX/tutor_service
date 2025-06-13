package de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.Template;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.CategorizedQuestionEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateMapper {

    private final ModelMapper modelMapper;

    public Template entityToDto(CategorizedQuestionEntity templateEntity) {
        // add specific mapping here if needed
        return modelMapper.map(templateEntity, Template.class);
    }

    public CategorizedQuestionEntity dtoToEntity(Template template) {
        // add specific mapping here if needed
        return modelMapper.map(template, CategorizedQuestionEntity.class);
    }
}
