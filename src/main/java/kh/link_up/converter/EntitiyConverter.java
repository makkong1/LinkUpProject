package kh.link_up.converter;

public interface EntitiyConverter<E, D> {
    D convertToDTO(E entity);
    E convertToEntity(D dto);
}
